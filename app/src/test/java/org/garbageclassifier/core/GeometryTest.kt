package org.garbageclassifier.core

import org.junit.Assert.*
import org.junit.Test

class GeometryTest {
    @Test fun landscapeLetterboxHasVerticalPadding() {
        val mapping = Letterbox(640, 480, 320)
        assertEquals(0.5f, mapping.scale)
        assertEquals(0f, mapping.padX)
        assertEquals(40f, mapping.padY)
        assertEquals(Box(0f, 0f, 640f, 480f), mapping.toSource(Box(0f, 40f, 320f, 280f)))
    }

    @Test fun portraitLetterboxHasHorizontalPadding() {
        val mapping = Letterbox(480, 640, 320)
        assertEquals(40f, mapping.padX)
        assertEquals(0f, mapping.padY)
        assertEquals(Box(0f, 0f, 480f, 640f), mapping.toSource(Box(40f, 0f, 280f, 320f)))
    }

    @Test fun boxesRoundTripAcrossAspectRatios() {
        for ((width, height) in listOf(640 to 480, 480 to 640, 1920 to 1080, 1080 to 2400)) {
            val mapping = Letterbox(width, height, 320)
            val source = Box(width * 0.1f, height * 0.2f, width * 0.8f, height * 0.9f)
            val result = mapping.toSource(mapping.toModel(source))
            assertBox(source, result)
        }
    }

    @Test fun paddingOnlyBoxBecomesEmpty() {
        val result = Letterbox(640, 480, 320).toSource(Box(0f, 0f, 100f, 30f))
        assertFalse(result.isValid)
    }

    @Test fun clippingDoesNotStretchVisibleBox() {
        assertEquals(Box(0f, 10f, 100f, 100f), Box(-10f, 10f, 110f, 120f).clipped(100f, 100f))
    }

    @Test fun eachCameraRotationIsInvertible() {
        for (degrees in listOf(0, 90, 180, 270)) {
            for (point in listOf(Point(0f, 0f), Point(640f, 480f), Point(123f, 312f))) {
                val rotated = FrameRotation.upright(point, 640, 480, degrees)
                assertEquals(point, FrameRotation.buffer(rotated, 640, 480, degrees))
            }
        }
    }

    @Test fun ninetyDegreeBoxReturnsToBufferCoordinates() {
        val upright = Box(80f, 100f, 280f, 300f)
        assertEquals(Box(100f, 200f, 300f, 400f), FrameRotation.toBuffer(upright, 640, 480, 90))
    }

    @Test fun twoSeventyDegreeBoxReturnsToBufferCoordinates() {
        val upright = Box(80f, 100f, 280f, 300f)
        assertEquals(Box(340f, 80f, 540f, 280f), FrameRotation.toBuffer(upright, 640, 480, 270))
    }

    @Test fun oneEightyDegreeBoxReturnsToBufferCoordinates() {
        assertEquals(Box(340f, 180f, 540f, 380f),
            FrameRotation.toBuffer(Box(100f, 100f, 300f, 300f), 640, 480, 180))
    }

    @Test fun intersectionHandlesTouchingAndInvalidBoxes() {
        val box = Box(0f, 0f, 10f, 10f)
        assertEquals(1f, box.intersectionOverUnion(box))
        assertEquals(0f, box.intersectionOverUnion(Box(10f, 0f, 20f, 10f)))
        assertEquals(0f, Box(0f, 0f, 0f, 0f).intersectionOverUnion(Box(0f, 0f, 0f, 0f)))
        assertFalse(Box(Float.NaN, 0f, 5f, 5f).isValid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDimensionsAreRejected() { Letterbox(0, 480, 320) }

    @Test(expected = IllegalStateException::class)
    fun unexpectedRotationIsRejected() { FrameRotation.buffer(Point(0f, 0f), 10, 10, 45) }

    private fun assertBox(expected: Box, actual: Box) {
        assertEquals(expected.left, actual.left, 0.001f)
        assertEquals(expected.top, actual.top, 0.001f)
        assertEquals(expected.right, actual.right, 0.001f)
        assertEquals(expected.bottom, actual.bottom, 0.001f)
    }
}
