package org.garbageclassifier.camera

import android.graphics.Bitmap
import org.garbageclassifier.core.Box
import org.garbageclassifier.core.ObjectAppearance

object AppearanceSampler {
    fun sample(bitmap: Bitmap, box: Box, focusCenter: Boolean = false): ObjectAppearance {
        val samples = FloatArray(48)
        var index = 0
        for (row in 0 until 4) {
            for (column in 0 until 4) {
                val x = (box.left + box.width * position(column, focusCenter)).toInt()
                    .coerceIn(0, bitmap.width - 1)
                val y = (box.top + box.height * position(row, focusCenter)).toInt()
                    .coerceIn(0, bitmap.height - 1)
                val pixel = bitmap.getPixel(x, y)
                samples[index++] = ((pixel shr 16) and 255) / 255f
                samples[index++] = ((pixel shr 8) and 255) / 255f
                samples[index++] = (pixel and 255) / 255f
            }
        }
        return ObjectAppearance(samples)
    }

    private fun position(index: Int, focusCenter: Boolean): Float =
        if (focusCenter && index in 1..2) if (index == 1) 0.48f else 0.52f
        else 0.2f + index * 0.2f
}
