package org.garbageclassifier.core

import org.junit.Assert.*
import org.junit.Test

class CenterEvidenceTest {
    private val center = Box(0f, 0f, 100f, 100f)
    private fun appearance(value: Float = 0.3f) = ObjectAppearance(FloatArray(48) { value })
    private fun scene(value: Float = 0.3f) = Detection(-1, 0f, 0f, center, appearance(value))
    private fun bottle(value: Float = 0.3f, box: Box = Box(45f, 10f, 55f, 90f)) =
        Detection(0, 0.6f, 0.1f, box, appearance(value))
    private fun recognition() = Recognition(bottle().copy(classId = 7, confidence = 0.95f, margin = 0.7f),
        Material(7, "Plastic", BinType.DRY), true)

    @Test fun narrowUprightAndHorizontalBottlesPassWithoutLoweringClassConfidence() {
        assertNotNull(CenterGate.select(listOf(bottle()), center))
        assertNotNull(CenterGate.select(listOf(bottle(box = Box(10f, 45f, 90f, 55f))), center))
    }

    @Test fun tinyOffcenterAndMultipleObjectsStillFail() {
        assertNull(CenterGate.select(listOf(bottle(box = Box(45f, 45f, 55f, 55f))), center))
        assertNull(CenterGate.select(listOf(bottle(box = Box(10f, 10f, 20f, 90f))), center))
        assertNull(CenterGate.select(listOf(bottle(), bottle(box = Box(65f, 10f, 75f, 90f))), center))
    }

    @Test fun stableSceneCanSubmitInferenceWithoutAnyYoloDetection() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), null, null, 0)
        evidence.observe(scene(), null, null, 400)
        assertTrue(evidence.ready(400))
        val ticket = evidence.ticket(scene(), null, 400)
        assertEquals("no_object_evidence", evidence.rejection(ticket, scene(), null, 800, 850))
        assertEquals("no_object_evidence", evidence.rejection(ticket, scene(), bottle(), 800, 850))
    }

    @Test fun briefDetectorLossKeepsPendingInferenceForTheSameVisibleAppearance() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), bottle(), null, 0)
        val ticket = evidence.ticket(scene(), bottle(), 0)
        assertFalse(evidence.observe(scene(), null, appearance(), 200))
        assertFalse(evidence.observe(scene(), null, appearance(), 500))
        assertFalse(evidence.observe(scene(), bottle(), null, 650))
        assertNull(evidence.rejection(ticket, scene(), bottle(), 600, 650))
    }

    @Test fun pendingResultIsNotAcceptedDuringTheDetectionGap() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), bottle(), null, 0)
        val ticket = evidence.ticket(scene(), bottle(), 0)
        evidence.observe(scene(), null, appearance(), 200)
        assertEquals("no_object_evidence", evidence.rejection(ticket, scene(), null, 200, 250))
    }

    @Test fun inferenceStartedDuringABriefLossRetainsVerifiedObjectEvidence() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), bottle(), null, 0)
        evidence.observe(scene(), null, appearance(), 200)
        val ticket = evidence.ticket(scene(), null, 200)
        assertNotNull(ticket.subject)
        evidence.observe(scene(), bottle(), null, 600)
        assertNull(evidence.rejection(ticket, scene(), bottle(), 600, 650))
    }

    @Test fun prolongedDetectorLossInvalidatesTheOldTicket() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), bottle(), null, 0)
        val ticket = evidence.ticket(scene(), bottle(), 0)
        evidence.observe(scene(), null, appearance(), 400)
        assertTrue(evidence.observe(scene(), null, appearance(), 800))
        evidence.observe(scene(), bottle(), null, 1000)
        assertEquals("identity_changed", evidence.rejection(ticket, scene(), bottle(), 1000, 1050))
    }

    @Test fun removalDespiteSameBackgroundInvalidatesPendingResultImmediately() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), bottle(), null, 0)
        val ticket = evidence.ticket(scene(), bottle(), 0)
        assertTrue(evidence.observe(scene(), null, appearance(0.9f), 200))
        assertEquals("object_removed", evidence.resetReason)
        assertEquals("identity_changed", evidence.rejection(ticket, scene(), bottle(), 200, 250))
    }

    @Test fun slowInferenceCanConfirmOnlyWithContinuousMatchingObservations() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), bottle(), null, 0)
        val ticket = evidence.ticket(scene(), bottle(), 0)
        for (time in 200L..8000L step 200) evidence.observe(scene(), bottle(), null, time)
        assertNull(evidence.rejection(ticket, scene(), bottle(), 8000, 8050))
        assertEquals("inference_too_old", evidence.rejection(ticket, scene(), bottle(), 15001, 15050))
        assertEquals("delivery_too_old", evidence.rejection(ticket, scene(), bottle(), 8000, 9000))
    }

    @Test fun cameraGapOrResetCannotValidateACompletedInference() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), bottle(), null, 0)
        val ticket = evidence.ticket(scene(), bottle(), 0)
        assertTrue(evidence.observe(scene(), bottle(), null, 2000))
        assertEquals("frame_gap", evidence.resetReason)
        assertEquals("identity_changed", evidence.rejection(ticket, scene(), bottle(), 2000, 2050))
        val next = evidence.ticket(scene(), bottle(), 2000)
        evidence.reset()
        evidence.observe(scene(), bottle(), null, 2200)
        assertEquals("identity_changed", evidence.rejection(next, scene(), bottle(), 2200, 2250))
    }

    @Test fun gradualAppearanceDriftCannotReplaceTheAnchoredObject() {
        val evidence = CenterEvidence()
        evidence.observe(scene(), bottle(), null, 0)
        val ticket = evidence.ticket(scene(), bottle(), 0)
        evidence.observe(scene(), bottle(0.4f), null, 200)
        assertTrue(evidence.observe(scene(), bottle(0.5f), null, 400))
        assertEquals("object_changed", evidence.resetReason)
        assertEquals("identity_changed", evidence.rejection(ticket, scene(), bottle(0.5f), 400, 450))
    }

    @Test fun briefLossPreservesFirstIndependentEvidenceWithoutDrawingABox() {
        val tracker = RecognitionTracker(CenterEvidence.EVIDENCE_LIFETIME_MS, 2)
        val first = tracker.update(listOf(recognition()), 0).single()
        assertNull(first.bin)
        assertTrue(tracker.update(emptyList(), 200, false, CenterEvidence.LOSS_GRACE_MS).isEmpty())
        val next = tracker.update(listOf(recognition()), 700).single()
        assertEquals(first.trackId, next.trackId)
        assertEquals(BinType.DRY, next.bin)
    }

    @Test fun longLossDropsUnconfirmedEvidence() {
        val tracker = RecognitionTracker(CenterEvidence.EVIDENCE_LIFETIME_MS, 2)
        val first = tracker.update(listOf(recognition()), 0).single()
        tracker.update(emptyList(), 800, false, CenterEvidence.LOSS_GRACE_MS)
        val next = tracker.update(listOf(recognition()), 1000).single()
        assertNotEquals(first.trackId, next.trackId)
        assertNull(next.bin)
    }

    @Test fun narrowBottleJitterDuringBriefLossKeepsItsIdentity() {
        val tracker = RecognitionTracker(CenterEvidence.EVIDENCE_LIFETIME_MS, 2)
        val first = tracker.update(listOf(recognition()), 0).single()
        tracker.update(emptyList(), 200, false, CenterEvidence.LOSS_GRACE_MS)
        val moved = recognition().let { it.copy(detection = it.detection.copy(box = Box(48f, 10f, 58f, 90f))) }
        val next = tracker.update(listOf(moved), 700, true, CenterEvidence.LOSS_GRACE_MS).single()
        assertEquals(first.trackId, next.trackId)
        assertEquals(BinType.DRY, next.bin)
    }

    @Test fun twoSlowIndependentResultsConfirmButReusedFramesNeverDo() {
        val tracker = RecognitionTracker(CenterEvidence.EVIDENCE_LIFETIME_MS, 2)
        tracker.update(listOf(recognition()), 8000)
        for (time in 8200L..15800L step 200) {
            assertNull(tracker.update(listOf(recognition()), time, false).single().bin)
        }
        assertEquals(BinType.DRY, tracker.update(listOf(recognition()), 16000).single().bin)
    }
}
