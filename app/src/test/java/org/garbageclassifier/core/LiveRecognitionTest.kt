package org.garbageclassifier.core

import org.junit.Assert.*
import org.junit.Test

class LiveRecognitionTest {
    private fun appearance(value: Float = 0.3f) = ObjectAppearance(FloatArray(48) { value })
    private fun scene(value: Float = 0.3f) = Detection(-1, 0f, 0f, Box(0f, 0f, 100f, 100f), appearance(value))
    private fun objectAt(x: Float, value: Float = 0.3f) =
        Detection(0, 0.8f, 0.2f, Box(x, 10f, x + 20, 90f), appearance(value))
    private fun classified() = Recognition(objectAt(40f).copy(classId = 7),
        Material(7, "Plastic", BinType.DRY), true, trackId = 1)

    @Test fun detectedUnclassifiedObjectHasNeutralRealCoordinatesAndNoBinAdvice() {
        val observation = Recognition(objectAt(40f), null, false)
        val tracker = RecognitionTracker(16000, 2)
        val tracked = tracker.update(listOf(observation), 0, false)
        val shown = CenterGate.withBoxLabels(tracked, observation).single().forOverlay()
        assertTrue(shown.visible)
        assertEquals(observation.detection.box, shown.detection.box)
        assertNull(shown.material)
        assertNull(shown.bin)
        assertEquals(SortingStatus.UNCERTAIN, WasteRules.inspect(shown, BinType.WET))
    }

    @Test fun verifiedMaterialKeepsTheExistingWrongBinStatus() {
        val confirmed = classified()
        val shown = CenterGate.withBoxLabels(listOf(confirmed), confirmed).single().forOverlay()
        assertEquals(BinType.DRY, shown.bin)
        assertEquals(SortingStatus.WRONG, WasteRules.inspect(shown, BinType.WET))
        assertEquals(SortingStatus.CORRECT, WasteRules.inspect(shown, BinType.DRY))
    }

    @Test fun heldCardDoesNotLabelAnUncertainOrContradictoryBox() {
        val held = classified()
        for (current in listOf(held.copy(confirmed = false),
            held.copy(detection = held.detection.copy(classId = 1)))) {
            val result = CenterGate.withBoxLabels(listOf(held), current).single()
            assertEquals(BinType.DRY, result.bin)
            assertNull(result.forOverlay().bin)
            assertEquals(SortingStatus.UNCERTAIN, WasteRules.inspect(result.forOverlay(), BinType.WET))
        }
    }

    @Test fun absentObjectsCannotProduceAVisibleRectangle() {
        val ghost = classified().copy(visible = false)
        val result = CenterGate.withBoxLabels(listOf(ghost), classified()).single()
        assertFalse(result.visible)
        assertNull(result.forOverlay().bin)
        assertTrue(CenterGate.withBoxLabels(emptyList(), classified()).isEmpty())
    }

    @Test fun continuousMotionAndBackgroundChangeDoNotDiscardPendingClassification() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), objectAt(30f), null, 0)
        val ticket = evidence.ticket(scene(), objectAt(30f), 0)
        for (step in 1..10) {
            assertFalse(evidence.observe(scene(0.9f), objectAt(30f + step * 2), null, step * 200L))
        }
        assertEquals(0f, ticket.subject!!.box.intersectionOverUnion(objectAt(50f).box), 0f)
        assertNull(evidence.rejection(ticket, scene(0.9f), objectAt(50f), 2000, 2050))
    }

    @Test fun appearanceReplacementDuringMotionStillInvalidatesTheTicket() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), objectAt(40f), null, 0)
        val ticket = evidence.ticket(scene(), objectAt(40f), 0)
        evidence.observe(scene(), objectAt(42f), null, 200)
        assertTrue(evidence.observe(scene(), objectAt(44f, 0.9f), null, 400))
        assertEquals("identity_changed", evidence.rejection(ticket, scene(), objectAt(44f, 0.9f), 400, 450))
    }

    @Test fun abruptPositionJumpCannotBorrowTheOldObjectsIdentity() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), objectAt(30f), null, 0)
        val ticket = evidence.ticket(scene(), objectAt(30f), 0)
        assertTrue(evidence.observe(scene(), objectAt(70f), null, 200))
        assertEquals("object_changed", evidence.resetReason)
        assertEquals("identity_changed", evidence.rejection(ticket, scene(), objectAt(70f), 200, 250))
    }

    @Test fun changedViewportCannotAcceptAnOldCenterCrop() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), objectAt(40f), null, 0)
        val ticket = evidence.ticket(scene(), objectAt(40f), 0)
        val movedViewport = scene().copy(box = Box(100f, 100f, 200f, 200f))
        assertEquals("viewport_changed", evidence.rejection(ticket, movedViewport, objectAt(40f), 200, 250))
    }
}
