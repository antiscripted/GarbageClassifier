package org.garbageclassifier.core

import kotlin.math.abs

class ObjectAppearance(private val samples: FloatArray) {
    init {
        require(samples.size == 48 && samples.all { it.isFinite() && it in 0f..1f })
    }

    fun distance(other: ObjectAppearance): Float =
        samples.indices.sumOf { abs(samples[it] - other.samples[it]).toDouble() }.toFloat() / samples.size
}

class RecognitionTracker {
    private data class Track(
        val id: Long,
        var observation: Recognition,
        var box: Box,
        var lastSeen: Long,
        var candidateClass: Int,
        var consistent: Int = 0,
        var consistentSince: Long = 0,
        var locked: Recognition? = null,
        var lockedAt: Long = 0,
        var contradictions: Int = 0,
        var contradictionSince: Long = 0
    )

    private val tracks = mutableListOf<Track>()
    private var nextId = 1L
    private var lastUpdate = Long.MIN_VALUE

    fun update(observations: List<Recognition>, timeMs: Long): List<Recognition> {
        if (lastUpdate != Long.MIN_VALUE && (timeMs < lastUpdate || timeMs - lastUpdate > 1500)) reset()
        lastUpdate = timeMs
        tracks.removeAll { expired(it, timeMs) }
        val available = tracks.toMutableList()
        val visible = mutableListOf<Recognition>()
        for (observation in observations.sortedByDescending { it.detection.confidence }) {
            val detection = observation.detection
            val matches = available.filter { matches(it, detection, timeMs) }
            val track = matches.singleOrNull()
            if (track == null) {
                val changed = available.filter {
                    it.observation.detection.box.intersectionOverUnion(detection.box) >= 0.35f &&
                        incompatibleAppearance(it, detection)
                }
                tracks.removeAll(changed.toSet())
                available.removeAll(changed.toSet())
                val created = Track(nextId++, observation, detection.box, timeMs, detection.classId)
                tracks += created
                updateConfirmation(created, observation, timeMs)
                visible += render(created, observation, true)
            } else {
                available.remove(track)
                if (timeMs - track.lastSeen > 700) track.consistent = 0
                track.lastSeen = timeMs
                track.box = smooth(track.box, detection.box)
                updateConfirmation(track, observation, timeMs)
                track.observation = observation
                visible += render(track, observation, true)
            }
        }
        for (track in available) {
            track.consistent = 0
            if (track.locked != null && !expired(track, timeMs)) {
                visible += render(track, track.observation, false)
            }
        }
        tracks.removeAll { it in available && it.locked == null }
        return visible
    }

    private fun matches(track: Track, detection: Detection, timeMs: Long): Boolean {
        val previous = track.observation.detection
        val reference = track.locked?.detection?.appearance ?: previous.appearance ?: return false
        val appearance = detection.appearance ?: return false
        val minimumOverlap = if (timeMs - track.lastSeen > 500) 0.65f else 0.40f
        val areaRatio = detection.box.area / previous.box.area
        return areaRatio in 0.5f..2f &&
            previous.box.intersectionOverUnion(detection.box) >= minimumOverlap &&
            reference.distance(appearance) <= 0.14f
    }

    private fun incompatibleAppearance(track: Track, detection: Detection): Boolean {
        val previous = track.locked?.detection?.appearance ?: track.observation.detection.appearance
        val current = detection.appearance
        return previous != null && current != null && previous.distance(current) > 0.14f
    }

    private fun updateConfirmation(track: Track, observation: Recognition, timeMs: Long) {
        val detection = observation.detection
        val locked = track.locked
        if (locked != null) {
            val contradictory = detection.classId != locked.detection.classId &&
                detection.confidence >= RecognitionFilter.CONFIDENCE &&
                detection.margin >= RecognitionFilter.MARGIN
            if (contradictory) {
                if (track.contradictions == 0) track.contradictionSince = timeMs
                track.contradictions++
                if (track.contradictions >= 3 && timeMs - track.contradictionSince >= 400) {
                    track.locked = null
                    track.consistent = 0
                    track.candidateClass = detection.classId
                }
            } else {
                track.contradictions = 0
            }
            return
        }
        if (!observation.confirmed || observation.bin == null || detection.appearance == null) {
            track.consistent = 0
            return
        }
        if (track.candidateClass != detection.classId) {
            track.candidateClass = detection.classId
            track.consistent = 0
        }
        if (track.consistent == 0) track.consistentSince = timeMs
        track.consistent++
        if (track.consistent >= 3 && timeMs - track.consistentSince >= 400) {
            track.locked = observation
            track.lockedAt = timeMs
            track.contradictions = 0
        }
    }

    private fun render(track: Track, observation: Recognition, visible: Boolean): Recognition {
        val locked = track.locked
        return if (locked == null) {
            observation.copy(detection = observation.detection.copy(box = track.box),
                confirmed = false, trackId = track.id, visible = visible)
        } else {
            locked.copy(detection = locked.detection.copy(box = track.box),
                trackId = track.id, visible = visible)
        }
    }

    private fun expired(track: Track, timeMs: Long): Boolean =
        track.locked != null && timeMs - track.lockedAt >= MINIMUM_HOLD_MS &&
            timeMs - track.lastSeen >= MISSING_TIMEOUT_MS

    private fun smooth(previous: Box, current: Box): Box {
        fun blend(before: Float, after: Float) = before + (after - before) * 0.35f
        return Box(blend(previous.left, current.left), blend(previous.top, current.top),
            blend(previous.right, current.right), blend(previous.bottom, current.bottom))
    }

    fun reset() {
        tracks.clear()
        lastUpdate = Long.MIN_VALUE
    }

    companion object {
        const val MINIMUM_HOLD_MS = 4000L
        const val MISSING_TIMEOUT_MS = 1250L
    }
}
