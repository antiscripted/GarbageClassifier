package org.garbageclassifier.core

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

object AntialiasedResize {
    private data class Weights(val first: Int, val values: FloatArray)

    fun square(pixels: IntArray, side: Int, target: Int): IntArray {
        require(side > 0 && target > 0 && pixels.size == side * side)
        val scale = side.toFloat() / target
        val radius = maxOf(1f, scale)
        val weights = List(target) { index ->
            val center = (index + 0.5f) * scale
            val first = floor(center - radius + 0.5f).toInt().coerceAtLeast(0)
            val end = floor(center + radius + 0.5f).toInt().coerceAtMost(side)
            val values = FloatArray(end - first) { offset ->
                (1f - abs(first + offset + 0.5f - center) / radius).coerceAtLeast(0f)
            }
            val total = values.sum()
            for (offset in values.indices) values[offset] /= total
            Weights(first, values)
        }
        val horizontal = IntArray(side * target)
        for (y in 0 until side) {
            for (x in 0 until target) horizontal[y * target + x] = blend(pixels, y * side, 1, weights[x])
        }
        return IntArray(target * target) { index ->
            blend(horizontal, index % target, target, weights[index / target])
        }
    }

    private fun blend(pixels: IntArray, offset: Int, stride: Int, weights: Weights): Int {
        var red = 0f
        var green = 0f
        var blue = 0f
        for (index in weights.values.indices) {
            val pixel = pixels[offset + (weights.first + index) * stride]
            val weight = weights.values[index]
            red += ((pixel shr 16) and 255) * weight
            green += ((pixel shr 8) and 255) * weight
            blue += (pixel and 255) * weight
        }
        return (255 shl 24) or (red.roundToInt().coerceIn(0, 255) shl 16) or
            (green.roundToInt().coerceIn(0, 255) shl 8) or blue.roundToInt().coerceIn(0, 255)
    }
}
