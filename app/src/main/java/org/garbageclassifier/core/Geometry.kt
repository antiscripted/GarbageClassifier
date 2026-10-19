package org.garbageclassifier.core

import kotlin.math.max
import kotlin.math.min

data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val area: Float get() = max(0f, width) * max(0f, height)
    val isValid: Boolean
        get() = listOf(left, top, right, bottom).all { it.isFinite() } && width > 0 && height > 0

    fun clipped(width: Float, height: Float): Box = Box(
        left.coerceIn(0f, width),
        top.coerceIn(0f, height),
        right.coerceIn(0f, width),
        bottom.coerceIn(0f, height)
    )

    fun intersectionOverUnion(other: Box): Float {
        val intersection = max(0f, min(right, other.right) - max(left, other.left)) *
            max(0f, min(bottom, other.bottom) - max(top, other.top))
        val union = area + other.area - intersection
        return if (union > 0) intersection / union else 0f
    }
}

data class Letterbox(val width: Int, val height: Int, val inputSize: Int) {
    init {
        require(width > 0 && height > 0 && inputSize > 0)
    }

    val scale = min(inputSize.toFloat() / width, inputSize.toFloat() / height)
    val padX = (inputSize - width * scale) / 2f
    val padY = (inputSize - height * scale) / 2f

    fun toSource(box: Box): Box = Box(
        (box.left - padX) / scale,
        (box.top - padY) / scale,
        (box.right - padX) / scale,
        (box.bottom - padY) / scale
    ).clipped(width.toFloat(), height.toFloat())

    fun toModel(box: Box): Box = Box(
        box.left * scale + padX,
        box.top * scale + padY,
        box.right * scale + padX,
        box.bottom * scale + padY
    )
}

data class Point(val x: Float, val y: Float)

object FrameRotation {
    fun upright(point: Point, width: Int, height: Int, degrees: Int): Point = when (degrees) {
        0 -> point
        90 -> Point(height - point.y, point.x)
        180 -> Point(width - point.x, height - point.y)
        270 -> Point(point.y, width - point.x)
        else -> error("Unsupported camera rotation: $degrees")
    }

    fun buffer(point: Point, width: Int, height: Int, degrees: Int): Point = when (degrees) {
        0 -> point
        90 -> Point(point.y, height - point.x)
        180 -> Point(width - point.x, height - point.y)
        270 -> Point(width - point.y, point.x)
        else -> error("Unsupported camera rotation: $degrees")
    }

    fun toBuffer(box: Box, width: Int, height: Int, degrees: Int): Box {
        val corners = listOf(
            Point(box.left, box.top), Point(box.right, box.top),
            Point(box.right, box.bottom), Point(box.left, box.bottom)
        ).map { buffer(it, width, height, degrees) }
        return Box(corners.minOf { it.x }, corners.minOf { it.y },
            corners.maxOf { it.x }, corners.maxOf { it.y })
    }
}
