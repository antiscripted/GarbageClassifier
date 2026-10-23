package org.garbageclassifier.core

import org.junit.Assert.*
import org.junit.Test

class CenterScannerTest {
    private val center = Box(10f, 10f, 90f, 90f)
    private fun detection(box: Box, label: Int = 7) = Detection(label, 0.95f, 0.8f, box,
        ObjectAppearance(FloatArray(48) { 0.3f }))
    private fun recognition() = Recognition(detection(Box(20f, 20f, 80f, 80f)),
        ModelContract.materials[7], true)

    @Test fun centerGateRejectsEmptyBackgroundAndTinyPrintedDetail() {
        assertNull(CenterGate.select(emptyList(), center))
        assertNull(CenterGate.select(listOf(detection(Box(45f, 45f, 55f, 55f))), center))
    }

    @Test fun centerGateAcceptsOneLargeObjectWithoutUsingItsMaterialClass() {
        val box = Box(20f, 20f, 80f, 80f)
        assertEquals(box, CenterGate.select(listOf(detection(box, 0)), center)?.box)
    }

    @Test fun centerGateRejectsSeparateObjectsAndOffcenterObjects() {
        assertNull(CenterGate.select(listOf(detection(Box(10f, 10f, 48f, 90f)),
            detection(Box(52f, 10f, 90f, 90f))), center))
        assertNull(CenterGate.select(listOf(detection(Box(10f, 10f, 45f, 90f))), center))
    }

    @Test fun duplicateClassProposalsDoNotBecomeSeparateObjects() {
        val box = Box(20f, 20f, 80f, 80f)
        assertNotNull(CenterGate.select(listOf(detection(box, 4), detection(box, 7)), center))
    }

    @Test fun cachedFramesCannotConfirmOneClassifierPrediction() {
        val tracker = RecognitionTracker(6000, 2)
        assertNull(tracker.update(listOf(recognition()), 0).single().bin)
        for (time in 200L..3000L step 200) {
            assertNull(tracker.update(listOf(recognition()), time, freshEvidence = false).single().bin)
        }
        assertEquals(BinType.DRY, tracker.update(listOf(recognition()), 3200).single().bin)
    }

    @Test fun centerLockLastsFourSecondsAndRemovalResetsIt() {
        val tracker = RecognitionTracker(6000, 2)
        tracker.update(listOf(recognition()), 0)
        val confirmed = tracker.update(listOf(recognition()), 700).single()
        assertEquals(BinType.DRY, confirmed.bin)
        for (time in 900L..4500L step 200) {
            val ghost = tracker.update(emptyList(), time, freshEvidence = false).single()
            assertFalse(ghost.visible)
            assertEquals(confirmed.trackId, ghost.trackId)
        }
        assertTrue(tracker.update(emptyList(), 4700, freshEvidence = false).isEmpty())
    }

    @Test fun expiredEvidenceCannotAcquireLockDespiteFreshCameraFrames() {
        val tracker = RecognitionTracker(2000, 2)
        tracker.update(listOf(recognition()), 0)
        for (time in 200L..3000L step 200) {
            tracker.update(listOf(recognition()), time, freshEvidence = false)
        }
        assertNull(tracker.update(listOf(recognition()), 3200).single().bin)
    }

    @Test fun differentObjectCannotInheritCenterLock() {
        val tracker = RecognitionTracker(6000, 2)
        tracker.update(listOf(recognition()), 0)
        val original = tracker.update(listOf(recognition()), 700).single()
        val replacement = recognition().let { it.copy(detection = it.detection.copy(
            appearance = ObjectAppearance(FloatArray(48) { 0.9f }))) }
        val next = tracker.update(listOf(replacement), 900, freshEvidence = false).single()
        assertNotEquals(original.trackId, next.trackId)
        assertNull(next.bin)
    }

    @Test fun rectangleRequiresTwoIndependentConfirmedResults() {
        val tracker = RecognitionTracker(6000, 2)
        val current = recognition()
        assertFalse(CenterGate.canLocalize(tracker.update(listOf(current), 0), current))
        assertFalse(CenterGate.canLocalize(tracker.update(listOf(current), 350, false), current))
        assertTrue(CenterGate.canLocalize(tracker.update(listOf(current), 700), current))
    }

    @Test fun confirmedRectangleUsesSmoothedDetectorCoordinates() {
        val tracker = RecognitionTracker(6000, 2)
        val current = recognition()
        tracker.update(listOf(current), 0)
        tracker.update(listOf(current), 700)
        val moved = current.copy(detection = current.detection.copy(box = Box(30f, 20f, 90f, 80f)))
        val tracked = tracker.update(listOf(moved), 900, false)
        assertTrue(CenterGate.canLocalize(tracked, moved))
        assertEquals(Box(23.5f, 20f, 83.5f, 80f), tracked.single().detection.box)
    }

    @Test fun inspectionAppliesBinStatusOnlyToTheConfirmedObject() {
        val current = recognition()
        assertTrue(CenterGate.canLocalize(listOf(current), current))
        assertEquals(SortingStatus.WRONG, WasteRules.inspect(current, BinType.WET))
        assertEquals(SortingStatus.CORRECT, WasteRules.inspect(current, BinType.DRY))
        assertFalse(CenterGate.canLocalize(listOf(current, current.copy(trackId = 2)), current))
    }

    @Test fun uncertainOrContradictoryEvidenceCannotLabelAHeldRectangle() {
        val locked = recognition()
        assertFalse(CenterGate.canLocalize(listOf(locked), locked.copy(confirmed = false)))
        val changed = locked.copy(detection = locked.detection.copy(classId = 4))
        assertFalse(CenterGate.canLocalize(listOf(locked), changed))
    }

    @Test fun changedAppearanceOrPositionCannotReuseAConfirmedRectangle() {
        val locked = recognition()
        val changed = locked.copy(detection = locked.detection.copy(
            appearance = ObjectAppearance(FloatArray(48) { 0.9f })))
        assertFalse(CenterGate.canLocalize(listOf(locked), changed))
        val displaced = locked.copy(detection = locked.detection.copy(box = Box(150f, 150f, 210f, 210f)))
        assertFalse(CenterGate.canLocalize(listOf(locked), displaced))
    }

    @Test fun removalKeepsTheCardButHidesItsRectangleAndResetClearsBoth() {
        val tracker = RecognitionTracker(6000, 2)
        val current = recognition()
        tracker.update(listOf(current), 0)
        tracker.update(listOf(current), 700)
        val held = tracker.update(emptyList(), 900, false)
        assertEquals(BinType.DRY, held.single().bin)
        assertFalse(CenterGate.canLocalize(held, current))
        tracker.reset()
        assertFalse(CenterGate.canLocalize(tracker.update(listOf(current), 1100), current))
    }
}
