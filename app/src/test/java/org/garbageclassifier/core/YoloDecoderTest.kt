package org.garbageclassifier.core

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class YoloDecoderTest {
    private val decoder = YoloDecoder()
    private val square = Letterbox(320, 320, 320)

    @Test fun channelsAreNormalizedXywhFollowedByTenClassScores() {
        val data = output()
        candidate(data, 0, 7, 0.9f)
        val detected = decoder.decode(data, square).single()
        assertEquals(7, detected.classId)
        assertEquals(0.9f, detected.confidence)
        assertEquals(Box(80f, 120f, 240f, 200f), detected.box)
    }

    @Test fun scoresAreAlreadyProbabilitiesWithoutObjectnessOrExtraSigmoid() {
        val data = output()
        candidate(data, 0, 0, 0.8f)
        data[5 * ModelContract.CANDIDATES] = 0.1f
        val detected = decoder.decode(data, square).single()
        assertEquals(0, detected.classId)
        assertEquals(0.8f, detected.confidence)
        assertEquals(0.7f, detected.margin, 0.0001f)
    }

    @Test fun portraitPaddingIsRemovedBeforeCameraCoordinates() {
        val data = output()
        candidate(data, 0, 1, 0.9f, width = 0.25f, height = 0.5f)
        assertEquals(Box(80f, 160f, 240f, 480f),
            decoder.decode(data, Letterbox(320, 640, 320)).single().box)
    }

    @Test fun duplicateBoxesOfSameClassAreSuppressed() {
        val data = output()
        candidate(data, 0, 7, 0.9f)
        candidate(data, 1, 7, 0.8f, x = 0.51f)
        assertEquals(1, decoder.decode(data, square).size)
    }

    @Test fun overlappingDifferentClassesArePreserved() {
        val data = output()
        candidate(data, 0, 7, 0.9f)
        candidate(data, 1, 9, 0.8f)
        assertEquals(listOf(7, 9), decoder.decode(data, square).map { it.classId })
    }

    @Test fun zeroTensorHasNoDetections() { assertTrue(decoder.decode(output(), square).isEmpty()) }

    @Test fun weakScoresAreDiscarded() {
        val data = output()
        candidate(data, 0, 7, 0.249f)
        assertTrue(decoder.decode(data, square).isEmpty())
    }

    @Test fun intermediateConfidenceIsRetainedAsUncertain() {
        val data = output()
        candidate(data, 0, 7, 0.3f)
        val result = decoder.decode(data, square)
        val filter = RecognitionFilter(ModelContract.materials)
        filter.update(result, 0)
        assertNull(filter.update(result, 200).single().bin)
    }

    @Test fun nonFiniteCoordinatesAreDiscarded() {
        val data = output()
        candidate(data, 0, 7, 0.9f, x = Float.NaN)
        assertTrue(decoder.decode(data, square).isEmpty())
    }

    @Test fun nonFiniteScoresAreDiscarded() {
        val data = output()
        candidate(data, 0, 7, 0.9f)
        data[4 * ModelContract.CANDIDATES] = Float.NaN
        assertTrue(decoder.decode(data, square).isEmpty())
    }

    @Test fun scoresOutsideProbabilityRangeAreDiscarded() {
        val data = output()
        candidate(data, 0, 7, 1.1f)
        assertTrue(decoder.decode(data, square).isEmpty())
    }

    @Test fun negativeAndZeroBoxSizesAreDiscarded() {
        val data = output()
        candidate(data, 0, 7, 0.9f, width = -0.2f)
        candidate(data, 1, 7, 0.9f, height = 0f)
        assertTrue(decoder.decode(data, square).isEmpty())
    }

    @Test fun predictionsOutsideImageAreClipped() {
        val data = output()
        candidate(data, 0, 7, 0.9f, x = 0f, width = 0.5f)
        assertEquals(0f, decoder.decode(data, square).single().box.left)
    }

    @Test fun tiesHaveNoConfidenceMargin() {
        val data = output()
        candidate(data, 0, 7, 0.9f)
        data[13 * ModelContract.CANDIDATES] = 0.9f
        assertEquals(0f, decoder.decode(data, square).single().margin)
    }

    @Test(expected = IllegalArgumentException::class)
    fun incompatibleOutputFailsClosed() { decoder.decode(FloatArray(84 * 2100), square) }

    @Test fun actualModelOutputDecodesMetalAndExpectedImagePosition() {
        val bytes = checkNotNull(javaClass.getResourceAsStream("/metal1-output.f32")).use { it.readBytes() }
        val values = FloatArray(bytes.size / 4)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(values)
        val first = decoder.decode(values, Letterbox(512, 384, 320)).first()
        assertEquals(4, first.classId)
        assertTrue(first.confidence > 0.9f)
        assertEquals(0f, first.box.left, 2f)
        assertEquals(0f, first.box.top, 3f)
        assertEquals(308f, first.box.right, 5f)
        assertEquals(284f, first.box.bottom, 5f)
    }

    @Test fun resultCountIsBounded() {
        val candidates = (0..100).map { Detection(7, 0.9f, 0.8f, Box(it * 20f, 0f, it * 20f + 10f, 10f)) }
        assertEquals(20, Suppression.apply(candidates).size)
    }

    private fun output() = FloatArray(ModelContract.CHANNELS * ModelContract.CANDIDATES)

    private fun candidate(data: FloatArray, index: Int, label: Int, score: Float,
                          x: Float = 0.5f, y: Float = 0.5f,
                          width: Float = 0.5f, height: Float = 0.25f) {
        val count = ModelContract.CANDIDATES
        data[index] = x
        data[count + index] = y
        data[2 * count + index] = width
        data[3 * count + index] = height
        data[(4 + label) * count + index] = score
    }
}
