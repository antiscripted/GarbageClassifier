package org.garbageclassifier.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import org.garbageclassifier.core.Box
import org.garbageclassifier.core.CenterEvidence
import org.garbageclassifier.core.CenterGate
import org.garbageclassifier.core.Detection
import org.garbageclassifier.core.ObjectAssociation
import org.garbageclassifier.core.Recognition
import org.garbageclassifier.core.RecognitionTracker
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

class CenterScanner(private val context: Context) : FrameProcessor {
    private data class Completed(val ticket: CenterEvidence.Ticket, val completedAt: Long,
                                 val prediction: Result<MaterialPrediction>)

    private val detector = TrashDetector(context)
    private val evidence = CenterEvidence()
    private val tracker = RecognitionTracker(CenterEvidence.EVIDENCE_LIFETIME_MS, 2)
    private val completed = AtomicReference<Completed?>()
    private val closed = AtomicBoolean()
    private var lastSubmitted = Long.MIN_VALUE
    private var cached: Recognition? = null
    private var cachedAt = Long.MIN_VALUE
    private var subject: Detection? = null
    private var frames = 0L
    private var detections = 0L
    private var submissions = 0L
    private var accepted = 0L
    private var lastDiagnostic = Long.MIN_VALUE

    init {
        CenterModelRunner.retain()
    }

    override fun process(bitmap: Bitmap, rotation: Int, crop: Rect, timeMs: Long): FrameResult {
        val side = (minOf(crop.width(), crop.height()) * 0.8f).roundToInt().coerceAtLeast(2)
        val left = crop.left + (crop.width() - side) / 2
        val top = crop.top + (crop.height() - side) / 2
        val center = Box(left.toFloat(), top.toFloat(), (left + side).toFloat(), (top + side).toFloat())
        val proposals = detector.detect(bitmap, rotation)
        val found = CenterGate.select(proposals, center)?.let {
            it.copy(appearance = AppearanceSampler.sample(bitmap, it.box))
        }
        val scene = Detection(-1, 0f, 0f, center, AppearanceSampler.sample(bitmap, center, focusCenter = true))
        val retainedAppearance = subject?.let { AppearanceSampler.sample(bitmap, it.box) }
        val now = SystemClock.elapsedRealtime()
        frames++
        if (found != null) detections++
        if (evidence.observe(scene, found, retainedAppearance, now)) {
            clearEvidence()
            if (found != null || evidence.resetReason == "frame_gap") tracker.reset()
            Log.i(TAG, "reset reason=${evidence.resetReason} generation=${evidence.generation}")
        }
        if (found != null) {
            if (subject == null) tracker.reset()
            subject = found
        }
        var fresh = false
        val pending = completed.get()
        if (pending != null && (found != null || now - pending.completedAt > CenterEvidence.LOSS_GRACE_MS)) {
            val result = completed.getAndSet(null)
            if (result != null) {
                val rejection = evidence.rejection(result.ticket, scene, found, result.completedAt,
                    SystemClock.elapsedRealtime())
                val prediction = result.prediction.getOrElse {
                    Log.e(TAG, "inference_failed", it)
                    throw it
                }
                if (rejection == null) {
                    val material = MaterialClassifier.materials[prediction.classId]
                    cached = Recognition(checkNotNull(found).copy(classId = prediction.classId,
                        confidence = prediction.confidence, margin = prediction.margin), material,
                        prediction.confidence >= 0.80f && prediction.margin >= 0.15f && material.bin != null)
                    cachedAt = result.completedAt
                    fresh = true
                    accepted++
                }
                Log.i(TAG, "result=${rejection ?: "accepted"} class=${prediction.classId} " +
                    "confidence=${prediction.confidence} margin=${prediction.margin} " +
                    "inference_ms=${prediction.inferenceMs} latency_ms=${result.completedAt - result.ticket.capturedAt}")
            }
        }
        if (cached != null && (now - cachedAt > CenterEvidence.EVIDENCE_LIFETIME_MS ||
                found != null && !ObjectAssociation.matches(checkNotNull(cached).detection, found, 0.40f))) {
            cached = null
            tracker.reset()
        }
        if (CenterModelRunner.available && completed.get() == null && evidence.ready(now) &&
            (lastSubmitted == Long.MIN_VALUE || now - lastSubmitted >= 700)) {
            val pixels = IntArray(side * side)
            bitmap.getPixels(pixels, 0, side, left, top, side, side)
            val ticket = evidence.ticket(scene, found, now)
            if (CenterModelRunner.submit(context, pixels, side, rotation) { prediction ->
                    if (!closed.get()) completed.set(Completed(ticket, SystemClock.elapsedRealtime(), prediction))
                }) {
                lastSubmitted = now
                submissions++
            }
        }
        val output = if (found == null) {
            FrameResult(tracker.update(emptyList(), now, freshEvidence = false,
                missingGraceMs = CenterEvidence.LOSS_GRACE_MS), localized = false)
        } else {
            val observation = cached?.let {
                it.copy(detection = it.detection.copy(box = found.box, appearance = found.appearance))
            } ?: Recognition(found.copy(classId = -1, confidence = 0f, margin = 0f), null, false)
            val tracked = tracker.update(listOf(observation), now, freshEvidence = fresh,
                missingGraceMs = CenterEvidence.LOSS_GRACE_MS)
            FrameResult(tracked, localized = CenterGate.canLocalize(tracked, observation))
        }
        if (lastDiagnostic == Long.MIN_VALUE || now - lastDiagnostic >= 5000) {
            Log.i(TAG, "frames=$frames proposals=${proposals.size} object_frames=$detections " +
                "submitted=$submissions accepted=$accepted busy=${!CenterModelRunner.available} " +
                "confirmed=${output.recognitions.any { it.bin != null }} localized=${output.localized}")
            lastDiagnostic = now
        }
        return output
    }

    private fun clearEvidence() {
        completed.set(null)
        cached = null
        cachedAt = Long.MIN_VALUE
        lastSubmitted = Long.MIN_VALUE
        subject = null
    }

    override fun reset() {
        evidence.reset()
        clearEvidence()
        tracker.reset()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            reset()
            detector.close()
            CenterModelRunner.release()
        }
    }

    companion object {
        private const val TAG = "GarbageClassifierScanner"
    }
}
