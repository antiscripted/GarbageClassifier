package org.garbageclassifier.core

import org.junit.Assert.*
import org.junit.Test

class RecognitionTrackerTest {
    private val box = Box(0f, 0f, 100f, 100f)

    private fun observation(
        classId: Int = 7,
        confirmed: Boolean = true,
        confidence: Float = 0.9f,
        color: Float = 0.2f,
        bounds: Box = box
    ) = Recognition(Detection(classId, confidence, 0.8f, bounds,
        ObjectAppearance(FloatArray(48) { color })), ModelContract.materials[classId], confirmed)

    private fun locked(tracker: RecognitionTracker): Recognition {
        tracker.update(listOf(observation()), 0)
        tracker.update(listOf(observation()), 200)
        return tracker.update(listOf(observation()), 400).single()
    }

    @Test fun acquisitionRequiresThreeConsistentConfirmedObservations() {
        val tracker = RecognitionTracker()
        assertNull(tracker.update(listOf(observation()), 0).single().bin)
        assertNull(tracker.update(listOf(observation()), 200).single().bin)
        assertEquals(BinType.DRY, tracker.update(listOf(observation()), 400).single().bin)
    }

    @Test fun livePipelineRequiresFourRawSightingsAcrossSixHundredMilliseconds() {
        val filter = RecognitionFilter(ModelContract.materials)
        val tracker = RecognitionTracker()
        for (time in listOf(0L, 200L, 400L)) {
            assertNull(tracker.update(filter.update(listOf(observation().detection), time), time).single().bin)
        }
        assertEquals(BinType.DRY, tracker.update(filter.update(
            listOf(observation().detection), 600), 600).single().bin)
    }

    @Test fun rapidFramesAloneCannotImmediatelyLock() {
        val tracker = RecognitionTracker()
        for (time in 0L..100L step 10) assertNull(tracker.update(listOf(observation()), time).single().bin)
    }

    @Test fun uncertaintyInterruptsAcquisition() {
        val tracker = RecognitionTracker()
        tracker.update(listOf(observation()), 0)
        tracker.update(listOf(observation(confirmed = false)), 200)
        assertNull(tracker.update(listOf(observation()), 400).single().bin)
        assertNull(tracker.update(listOf(observation()), 600).single().bin)
        assertNotNull(tracker.update(listOf(observation()), 800).single().bin)
    }

    @Test fun unsupportedClassCannotLock() {
        val tracker = RecognitionTracker()
        for (time in 0L..2000L step 200) assertNull(tracker.update(
            listOf(observation(classId = 0)), time).single().bin)
    }

    @Test fun appearanceIsRequiredForIdentityAndLocking() {
        val tracker = RecognitionTracker()
        val item = observation().let { it.copy(detection = it.detection.copy(appearance = null)) }
        for (time in 0L..2000L step 200) assertNull(tracker.update(listOf(item), time).single().bin)
    }

    @Test fun briefConfidenceDipDoesNotFlicker() {
        val tracker = RecognitionTracker()
        val reference = locked(tracker)
        val result = tracker.update(listOf(observation(confirmed = false, confidence = 0.3f)), 600).single()
        assertEquals(reference.trackId, result.trackId)
        assertEquals(BinType.DRY, result.bin)
        assertTrue(result.visible)
    }

    @Test fun temporaryOcclusionHoldsCardButHidesTheBox() {
        val tracker = RecognitionTracker()
        val reference = locked(tracker)
        val hidden = tracker.update(emptyList(), 600).single()
        assertEquals(BinType.DRY, hidden.bin)
        assertFalse(hidden.visible)
        val returned = tracker.update(listOf(observation()), 1000).single()
        assertTrue(returned.visible)
        assertEquals(reference.trackId, returned.trackId)
    }

    @Test fun missingObjectIsHeldForFourSecondsThenAutomaticallyCleared() {
        val tracker = RecognitionTracker()
        locked(tracker)
        for (time in 600L..4200L step 200) {
            val held = tracker.update(emptyList(), time).single()
            assertEquals(BinType.DRY, held.bin)
            assertFalse(held.visible)
        }
        assertTrue(tracker.update(emptyList(), 4400).isEmpty())
        assertNull(tracker.update(listOf(observation()), 4600).single().bin)
    }

    @Test fun visibleObjectRemainsLockedBeyondMinimumHold() {
        val tracker = RecognitionTracker()
        val id = locked(tracker).trackId
        for (time in 600L..10000L step 200) {
            val result = tracker.update(listOf(observation()), time).single()
            assertEquals(id, result.trackId)
            assertEquals(BinType.DRY, result.bin)
        }
    }

    @Test fun absenceAfterMinimumHoldStillHasAOneAndQuarterSecondGracePeriod() {
        val tracker = RecognitionTracker()
        locked(tracker)
        for (time in 600L..5000L step 200) tracker.update(listOf(observation()), time)
        for (time in 5200L..6200L step 200) assertNotNull(tracker.update(emptyList(), time).single().bin)
        assertTrue(tracker.update(emptyList(), 6400).isEmpty())
    }

    @Test fun oneContradictoryFrameCannotReplaceLockedMaterial() {
        val tracker = RecognitionTracker()
        locked(tracker)
        assertEquals(7, tracker.update(listOf(observation(classId = 1)), 600).single().material?.id)
        assertEquals(7, tracker.update(listOf(observation()), 800).single().material?.id)
    }

    @Test fun sustainedContradictionRevokesBeforeReacquisition() {
        val tracker = RecognitionTracker()
        locked(tracker)
        tracker.update(listOf(observation(classId = 1)), 600)
        tracker.update(listOf(observation(classId = 1)), 800)
        assertNull(tracker.update(listOf(observation(classId = 1)), 1000).single().bin)
        assertNull(tracker.update(listOf(observation(classId = 1)), 1200).single().bin)
        assertNull(tracker.update(listOf(observation(classId = 1)), 1400).single().bin)
        assertEquals(BinType.WET, tracker.update(listOf(observation(classId = 1)), 1600).single().bin)
    }

    @Test fun differentAppearanceAtTheSamePositionNeverInheritsTheLock() {
        val tracker = RecognitionTracker()
        val old = locked(tracker)
        val replacement = tracker.update(listOf(observation(color = 0.8f)), 600).single()
        assertNotEquals(old.trackId, replacement.trackId)
        assertNull(replacement.bin)
    }

    @Test fun differentPositionNeverInheritsTheLockEvenWithSameAppearance() {
        val tracker = RecognitionTracker()
        val old = locked(tracker)
        val result = tracker.update(listOf(observation(bounds = Box(200f, 0f, 300f, 100f))), 600)
        val replacement = result.single { it.visible }
        assertNotEquals(old.trackId, replacement.trackId)
        assertNull(replacement.bin)
        assertFalse(result.single { it.trackId == old.trackId }.visible)
    }

    @Test fun boundingBoxesMoveSmoothlyWithoutChangingIdentity() {
        val tracker = RecognitionTracker()
        val old = locked(tracker)
        val result = tracker.update(listOf(observation(bounds = Box(10f, 0f, 110f, 100f))), 600).single()
        assertEquals(old.trackId, result.trackId)
        assertEquals(3.5f, result.detection.box.left, 0.01f)
        assertEquals(103.5f, result.detection.box.right, 0.01f)
    }

    @Test fun simultaneousObjectsKeepIndependentBins() {
        val tracker = RecognitionTracker()
        val objects = listOf(observation(), observation(classId = 1, color = 0.7f,
            bounds = Box(200f, 0f, 300f, 100f)))
        tracker.update(objects, 0)
        tracker.update(objects, 200)
        val results = tracker.update(objects, 400)
        assertEquals(2, results.map { it.trackId }.distinct().size)
        for (bin in BinType.entries) {
            assertEquals(1, results.count { WasteRules.inspect(it, bin) == SortingStatus.WRONG })
            assertEquals(1, results.count { WasteRules.inspect(it, bin) == SortingStatus.CORRECT })
        }
    }

    @Test fun resetClearsHoldAndRequiresNewConfirmation() {
        val tracker = RecognitionTracker()
        val old = locked(tracker)
        tracker.reset()
        assertTrue(tracker.update(emptyList(), 600).isEmpty())
        val next = tracker.update(listOf(observation()), 800).single()
        assertNull(next.bin)
        assertNotEquals(old.trackId, next.trackId)
    }

    @Test fun clockRegressionAndLongFrameStallClearStaleLocks() {
        for (time in listOf(100L, 3000L)) {
            val tracker = RecognitionTracker()
            locked(tracker)
            assertNull(tracker.update(listOf(observation()), time).single().bin)
        }
    }

    @Test fun ambiguousCrossingRevokesBothLocksBeforeObjectsSeparate() {
        val tracker = RecognitionTracker()
        val objects = listOf(observation(bounds = Box(0f, 0f, 100f, 100f)),
            observation(bounds = Box(60f, 0f, 160f, 100f)))
        for (time in listOf(0L, 200L, 400L)) tracker.update(objects, time)
        val ambiguous = tracker.update(listOf(observation(bounds = Box(30f, 0f, 130f, 100f))), 600)
        assertTrue(ambiguous.all { it.bin == null })
        assertTrue(tracker.update(objects, 800).all { it.bin == null })
    }

    @Test fun oneTrackCannotBeAssignedToTwoOverlappingObjects() {
        val tracker = RecognitionTracker()
        locked(tracker)
        val objects = listOf(observation(bounds = Box(0f, 0f, 80f, 100f)),
            observation(bounds = Box(20f, 0f, 100f, 100f)))
        assertTrue(tracker.update(objects, 600).all { it.bin == null })
    }

    @Test fun gradualAppearanceDriftCannotMoveTheLockedIdentityToAnotherObject() {
        val tracker = RecognitionTracker()
        locked(tracker)
        assertNotNull(tracker.update(listOf(observation(color = 0.3f)), 600).single().bin)
        assertNull(tracker.update(listOf(observation(color = 0.4f)), 800).single().bin)
    }
}
