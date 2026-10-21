package org.garbageclassifier.core

object CenterGate {
    fun select(detections: List<Detection>, center: Box): Detection? {
        val significant = detections.filter { coverage(it.box, center) >= 0.15f }
            .map { it.copy(classId = 0) }
        val candidate = Suppression.apply(significant).singleOrNull() ?: return null
        val x = (center.left + center.right) / 2
        val y = (center.top + center.bottom) / 2
        return candidate.takeIf {
            x in it.box.left..it.box.right && y in it.box.top..it.box.bottom &&
                coverage(it.box, center) >= 0.25f
        }
    }

    private fun coverage(box: Box, center: Box): Float {
        val width = (minOf(box.right, center.right) - maxOf(box.left, center.left)).coerceAtLeast(0f)
        val height = (minOf(box.bottom, center.bottom) - maxOf(box.top, center.top)).coerceAtLeast(0f)
        return width * height / center.area
    }
}
