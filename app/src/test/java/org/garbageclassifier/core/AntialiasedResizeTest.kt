package org.garbageclassifier.core

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class AntialiasedResizeTest {
    @Test fun downsampleMatchesPillowBilinearReferenceWithinOneByte() {
        val source = IntArray(81) { index ->
            val x = index % 9
            val y = index / 9
            (255 shl 24) or (((x * 37 + y * 13) % 256) shl 16) or
                (((x * 11 + y * 47) % 256) shl 8) or ((x * 73 + y * 19) % 256)
        }
        val expected = intArrayOf(2764876, 7685773, 12017793, 4748919, 4492914, 9412454,
            10794379, 4235881, 6459020, 11229042, 8807811, 5001839, 8149649, 12611689, 5933697, 6725780)
        val actual = AntialiasedResize.square(source, 9, 4)
        for (index in expected.indices) for (shift in listOf(16, 8, 0)) {
            assertTrue(abs(((expected[index] shr shift) and 255) - ((actual[index] shr shift) and 255)) <= 1)
        }
    }

    @Test fun constantColorAndOnePixelInputsPreserveColor() {
        val color = 0xff1b83e7.toInt()
        assertTrue(AntialiasedResize.square(intArrayOf(color), 1, 224).all { it == color })
        assertTrue(AntialiasedResize.square(IntArray(1600) { color }, 40, 7).all { it == color })
    }
}
