package org.garbageclassifier.core

class CenterEvidence {
    data class Ticket(val generation: Long, val capturedAt: Long, val scene: Detection,
                      val subject: Detection?)

    var generation = 0L
        private set
    var resetReason = "initial"
        private set
    private var sceneAnchor: Detection? = null
    private var objectAnchor: Detection? = null
    private var lastObject: Detection? = null
    private var previousFrame = Long.MIN_VALUE
    private var lastDetected = Long.MIN_VALUE
    private var stableSince = Long.MIN_VALUE

    fun observe(scene: Detection, detected: Detection?, retainedAppearance: ObjectAppearance?,
                timeMs: Long): Boolean {
        val reason = when {
            previousFrame != Long.MIN_VALUE && timeMs - previousFrame !in 0..1500 -> "frame_gap"
            objectAnchor == null && sceneAnchor != null &&
                !ObjectAssociation.matches(checkNotNull(sceneAnchor), scene, 0.90f) -> "scene_changed"
            objectAnchor != null && detected != null &&
                !ObjectAssociation.matches(checkNotNull(lastObject), detected, 0.40f,
                    checkNotNull(objectAnchor).appearance) -> "object_changed"
            objectAnchor != null && detected == null &&
                (retainedAppearance == null || checkNotNull(objectAnchor).appearance?.distance(retainedAppearance)
                    ?.let { it > 0.14f } != false) -> "object_removed"
            objectAnchor != null && detected == null && timeMs - lastDetected > LOSS_GRACE_MS -> "detection_lost"
            else -> null
        }
        if (reason != null) reset(reason)
        if (sceneAnchor == null) {
            sceneAnchor = scene
            stableSince = timeMs
        }
        if (detected != null) {
            if (objectAnchor == null) objectAnchor = detected
            lastObject = detected
            lastDetected = timeMs
        }
        previousFrame = timeMs
        return reason != null
    }

    fun ready(timeMs: Long): Boolean = sceneAnchor != null && timeMs - stableSince >= 350

    fun ticket(scene: Detection, detected: Detection?, timeMs: Long) =
        Ticket(generation, timeMs, scene, detected ?: lastObject?.takeIf {
            timeMs - lastDetected in 0..LOSS_GRACE_MS
        })

    fun rejection(ticket: Ticket, scene: Detection, detected: Detection?, completedAt: Long,
                  timeMs: Long): String? = when {
        ticket.generation != generation -> "identity_changed"
        completedAt - ticket.capturedAt !in 0..MAX_INFERENCE_MS -> "inference_too_old"
        timeMs - completedAt !in 0..LOSS_GRACE_MS -> "delivery_too_old"
        ticket.scene.box.intersectionOverUnion(scene.box) < 0.90f -> "viewport_changed"
        ticket.subject == null || detected == null -> "no_object_evidence"
        lastObject == null || objectAnchor == null ||
            !ObjectAssociation.matches(checkNotNull(lastObject), detected, 0.40f,
                checkNotNull(objectAnchor).appearance) -> "object_mismatch"
        ticket.subject.appearance?.distance(checkNotNull(detected.appearance))
            ?.let { it > 0.14f } != false -> "object_mismatch"
        else -> null
    }

    fun reset(reason: String = "reset") {
        generation++
        resetReason = reason
        sceneAnchor = null
        objectAnchor = null
        lastObject = null
        previousFrame = Long.MIN_VALUE
        lastDetected = Long.MIN_VALUE
        stableSince = Long.MIN_VALUE
    }

    companion object {
        const val LOSS_GRACE_MS = 750L
        const val MAX_INFERENCE_MS = 15000L
        const val EVIDENCE_LIFETIME_MS = 16000L
    }
}
