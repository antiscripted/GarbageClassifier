package org.garbageclassifier.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import org.garbageclassifier.core.Box
import org.garbageclassifier.core.CenterGate
import org.garbageclassifier.core.Detection
import org.garbageclassifier.core.ObjectAssociation
import org.garbageclassifier.core.Recognition
import org.garbageclassifier.core.RecognitionTracker
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

class CenterScanner(private val context: Context) : FrameProcessor {
    private data class Completed(val epoch: Long, val capturedAt: Long, val subject: Detection,
                                 val prediction: Result<MaterialPrediction>)

    private val detector = TrashDetector(context)
    private val tracker = RecognitionTracker(maximumEvidenceGapMs = 6000, requiredObservations = 2)
    private val completed = AtomicReference<Completed?>()
    private val closed = AtomicBoolean()
    private var epoch = 0L
    private var lastSubmitted = Long.MIN_VALUE
    private var cached: Recognition? = null
    private var cachedAt = Long.MIN_VALUE
    private var subject: Detection? = null
    private var stableSince = Long.MIN_VALUE

    init {
        CenterModelRunner.retain()
    }

    override fun process(bitmap: Bitmap, rotation: Int, crop: Rect, timeMs: Long): FrameResult {
        val side = (minOf(crop.width(), crop.height()) * 0.8f).roundToInt().coerceAtLeast(2)
        val left = crop.left + (crop.width() - side) / 2
        val top = crop.top + (crop.height() - side) / 2
        val center = Box(left.toFloat(), top.toFloat(), (left + side).toFloat(), (top + side).toFloat())
        val found = CenterGate.select(detector.detect(bitmap, rotation), center)?.let {
            it.copy(appearance = AppearanceSampler.sample(bitmap, it.box))
        }
        if (found == null) {
            if (subject != null) invalidateEvidence()
            subject = null
            stableSince = Long.MIN_VALUE
            return FrameResult(tracker.update(emptyList(), timeMs, freshEvidence = false), localized = false)
        }
        val previous = subject
        if (previous == null || !ObjectAssociation.matches(previous, found, 0.40f)) {
            invalidateEvidence()
            tracker.reset()
            stableSince = timeMs
        }
        subject = found
        var fresh = false
        val result = completed.getAndSet(null)
        if (result != null && result.epoch == epoch && timeMs - result.capturedAt in 0..MAX_AGE_MS &&
            ObjectAssociation.matches(result.subject, found, 0.40f)) {
            val prediction = result.prediction.getOrThrow()
            val material = MaterialClassifier.materials[prediction.classId]
            cached = Recognition(found.copy(classId = prediction.classId, confidence = prediction.confidence,
                margin = prediction.margin), material,
                prediction.confidence >= 0.80f && prediction.margin >= 0.15f && material.bin != null)
            cachedAt = result.capturedAt
            fresh = true
        }
        if (cached != null && (timeMs - cachedAt > MAX_AGE_MS ||
                !ObjectAssociation.matches(checkNotNull(cached).detection, found, 0.40f))) {
            invalidateEvidence()
            tracker.reset()
        }
        if (CenterModelRunner.available && timeMs - stableSince >= 350 &&
            (lastSubmitted == Long.MIN_VALUE || timeMs - lastSubmitted >= 700)) {
            val pixels = IntArray(side * side)
            bitmap.getPixels(pixels, 0, side, left, top, side, side)
            val submittedEpoch = epoch
            if (CenterModelRunner.submit(context, pixels, side, rotation) { prediction ->
                    if (!closed.get()) completed.set(Completed(submittedEpoch, timeMs, found, prediction))
                }) lastSubmitted = timeMs
        }
        val observation = cached?.let {
            it.copy(detection = it.detection.copy(box = found.box, appearance = found.appearance))
        } ?: Recognition(found.copy(classId = -1, confidence = 0f, margin = 0f), null, false)
        return FrameResult(tracker.update(listOf(observation), timeMs, freshEvidence = fresh), localized = false)
    }

    private fun invalidateEvidence() {
        epoch++
        completed.set(null)
        cached = null
        cachedAt = Long.MIN_VALUE
        lastSubmitted = Long.MIN_VALUE
    }

    override fun reset() {
        invalidateEvidence()
        tracker.reset()
        subject = null
        stableSince = Long.MIN_VALUE
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            reset()
            detector.close()
            CenterModelRunner.release()
        }
    }

    companion object {
        private const val MAX_AGE_MS = 6000L
    }
}
