package org.garbageclassifier.camera

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.camera.view.transform.OutputTransform
import org.garbageclassifier.core.Recognition
import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong

@androidx.annotation.OptIn(TransformExperimental::class)
class CameraAnalyzer(
    private val context: Context,
    private val onFrame: (AnalyzedFrame) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer, Closeable {
    private var processor: FrameProcessor? = null
    private val resetGeneration = AtomicLong()
    private var appliedGeneration = 0L
    val trackingGeneration: Long get() = resetGeneration.get()
    private val transforms = ImageProxyTransformFactory()
    private var lastFrame = Long.MIN_VALUE
    private var failed = false

    override fun analyze(image: ImageProxy) {
        try {
            val now = SystemClock.elapsedRealtime()
            if (failed || lastFrame != Long.MIN_VALUE && now - lastFrame < 180) return
            lastFrame = now
            val generation = trackingGeneration
            if (generation != appliedGeneration) {
                processor?.reset()
                appliedGeneration = generation
            }
            val engine = processor ?: ScannerFactory.create(context).also { processor = it }
            val transform = transforms.getOutputTransform(image)
            val bitmap = image.toBitmap()
            val result = try {
                engine.process(bitmap, image.imageInfo.rotationDegrees, image.cropRect, now)
            } finally {
                bitmap.recycle()
            }
            onFrame(AnalyzedFrame(result.recognitions, transform, now, generation, result.localized))
        } catch (error: Exception) {
            failed = true
            processor?.reset()
            onError(error)
        } finally {
            image.close()
        }
    }

    fun resetTracking() {
        resetGeneration.incrementAndGet()
    }

    override fun close() {
        processor?.close()
        processor = null
    }
}

@androidx.annotation.OptIn(TransformExperimental::class)
data class AnalyzedFrame(
    val recognitions: List<Recognition>,
    val transform: OutputTransform,
    val capturedAtMs: Long,
    val trackingGeneration: Long,
    val localized: Boolean = true
)
