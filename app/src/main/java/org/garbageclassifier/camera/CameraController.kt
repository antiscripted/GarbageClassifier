package org.garbageclassifier.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.Closeable
import java.util.concurrent.Executors

class CameraController(
    private val context: Context,
    private val owner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onFrame: (AnalyzedFrame) -> Unit,
    private val onCameraError: (Exception) -> Unit,
    private val onDetectorError: (Exception) -> Unit
) : Closeable {
    private val worker = Executors.newSingleThreadExecutor()
    private val main = ContextCompat.getMainExecutor(context)
    private var boundCamera: Camera? = null
    private var provider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var analysis: ImageAnalysis? = null
    private var analyzer: CameraAnalyzer? = null
    private var generation = 0
    private var active = false

    fun start() {
        if (active || previewView.width == 0 || previewView.height == 0) return
        active = true
        val current = ++generation
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!active || current != generation) return@addListener
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                check(cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
                val rotation = previewView.display.rotation
                val cameraPreview = Preview.Builder().setTargetRotation(rotation).build()
                cameraPreview.surfaceProvider = previewView.surfaceProvider
                val cameraAnalysis = ImageAnalysis.Builder()
                    .setTargetRotation(rotation)
                    .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
                        ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
                    ).build())
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                val cameraAnalyzer = CameraAnalyzer(context.applicationContext,
                    { frame -> main.execute {
                        if (active && current == generation &&
                            frame.trackingGeneration == analyzer?.trackingGeneration) onFrame(frame)
                    } },
                    { error -> main.execute {
                        if (active && current == generation) onDetectorError(error)
                    } })
                analyzer = cameraAnalyzer
                preview = cameraPreview
                analysis = cameraAnalysis
                cameraAnalysis.setAnalyzer(worker, cameraAnalyzer)
                val viewport = checkNotNull(previewView.viewPort)
                val group = UseCaseGroup.Builder().setViewPort(viewport)
                    .addUseCase(cameraPreview).addUseCase(cameraAnalysis).build()
                val camera = cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, group)
                boundCamera = camera
                camera.cameraInfo.cameraState.observe(owner) { state ->
                    if (active && current == generation && state.error != null) {
                        onCameraError(IllegalStateException("Camera error ${state.error?.code}"))
                    }
                }
            } catch (error: Exception) {
                stop()
                onCameraError(error)
            }
        }, main)
    }

    fun resetTracking() {
        analyzer?.resetTracking()
    }

    fun stop() {
        active = false
        generation++
        boundCamera?.cameraInfo?.cameraState?.removeObservers(owner)
        boundCamera = null
        analysis?.clearAnalyzer()
        val useCases = listOfNotNull(preview, analysis).toTypedArray()
        if (useCases.isNotEmpty()) provider?.unbind(*useCases)
        preview = null
        analysis = null
        analyzer?.let { old -> worker.execute { old.close() } }
        analyzer = null
    }

    override fun close() {
        stop()
        worker.shutdown()
    }
}
