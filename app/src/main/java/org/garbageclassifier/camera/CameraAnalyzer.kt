package org.garbageclassifier.camera

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.TransformExperimental
import androidx.camera.view.transform.ImageProxyTransformFactory
import androidx.camera.view.transform.OutputTransform
import org.garbageclassifier.core.ModelContract
import org.garbageclassifier.core.Recognition
import org.garbageclassifier.core.RecognitionFilter
import java.io.Closeable

@androidx.annotation.OptIn(TransformExperimental::class)
class CameraAnalyzer(
    private val context: Context,
    private val onFrame: (AnalyzedFrame) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer, Closeable {
    private var detector: TrashDetector? = null
    private val filter = RecognitionFilter(ModelContract.materials)
    private val transforms = ImageProxyTransformFactory()
    private var lastFrame = Long.MIN_VALUE
    private var failed = false

    override fun analyze(image: ImageProxy) {
        try {
            val now = SystemClock.elapsedRealtime()
            if (failed || lastFrame != Long.MIN_VALUE && now - lastFrame < 180) return
            lastFrame = now
            val engine = detector ?: TrashDetector(context).also { detector = it }
            val transform = transforms.getOutputTransform(image)
            val bitmap = image.toBitmap()
            val detections = try {
                engine.detect(bitmap, image.imageInfo.rotationDegrees)
            } finally {
                bitmap.recycle()
            }
            onFrame(AnalyzedFrame(filter.update(detections, now), transform, now))
        } catch (error: Exception) {
            failed = true
            filter.reset()
            onError(error)
        } finally {
            image.close()
        }
    }

    override fun close() {
        detector?.close()
        detector = null
        filter.reset()
    }
}

@androidx.annotation.OptIn(TransformExperimental::class)
data class AnalyzedFrame(
    val recognitions: List<Recognition>,
    val transform: OutputTransform,
    val capturedAtMs: Long
)
