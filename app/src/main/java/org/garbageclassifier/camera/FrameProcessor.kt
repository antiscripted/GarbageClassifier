package org.garbageclassifier.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import org.garbageclassifier.core.ModelContract
import org.garbageclassifier.core.Recognition
import org.garbageclassifier.core.RecognitionFilter
import org.garbageclassifier.core.RecognitionTracker
import java.io.Closeable

interface FrameProcessor : Closeable {
    fun process(bitmap: Bitmap, rotation: Int, crop: Rect, timeMs: Long): FrameResult
    fun reset()
}

data class FrameResult(val recognitions: List<Recognition>, val localized: Boolean = true)

class TrackedDetector(context: Context) : FrameProcessor {
    private val detector = TrashDetector(context)
    private val filter = RecognitionFilter(ModelContract.materials)
    private val tracker = RecognitionTracker()

    override fun process(bitmap: Bitmap, rotation: Int, crop: Rect, timeMs: Long): FrameResult {
        val detections = detector.detect(bitmap, rotation).map {
            it.copy(appearance = AppearanceSampler.sample(bitmap, it.box))
        }
        return FrameResult(tracker.update(filter.update(detections, timeMs), timeMs))
    }

    override fun reset() {
        filter.reset()
        tracker.reset()
    }

    override fun close() {
        detector.close()
        reset()
    }
}
