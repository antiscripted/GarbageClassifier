package org.garbageclassifier.core

object CenterGate {
    fun withBoxLabels(tracked: List<Recognition>, current: Recognition): List<Recognition> {
        val verified = canLocalize(tracked, current)
        return tracked.map { it.copy(boxLabelVerified = verified && it.visible) }
    }

    fun canLocalize(tracked: List<Recognition>, current: Recognition): Boolean {
        val visible = tracked.filter { it.visible }.singleOrNull() ?: return false
        return current.bin != null && visible.bin != null &&
            visible.detection.classId == current.detection.classId &&
            ObjectAssociation.matches(visible.detection, current.detection, 0.40f)
    }

    fun select(detections: List<Detection>, center: Box): Detection? {
        val significant = detections.filter { it.box.isValid && coverage(it.box, center) >= 0.04f }
            .map { it.copy(classId = 0) }
        val candidate = Suppression.apply(significant).singleOrNull() ?: return null
        val x = (center.left + center.right) / 2
        val y = (center.top + center.bottom) / 2
        val width = (minOf(candidate.box.right, center.right) - maxOf(candidate.box.left, center.left)) / center.width
        val height = (minOf(candidate.box.bottom, center.bottom) - maxOf(candidate.box.top, center.top)) / center.height
        return candidate.takeIf {
            x in it.box.left..it.box.right && y in it.box.top..it.box.bottom &&
                (coverage(it.box, center) >= 0.25f ||
                    minOf(width, height) >= 0.08f && maxOf(width, height) >= 0.45f)
        }
    }

    private fun coverage(box: Box, center: Box): Float {
        val width = (minOf(box.right, center.right) - maxOf(box.left, center.left)).coerceAtLeast(0f)
        val height = (minOf(box.bottom, center.bottom) - maxOf(box.top, center.top)).coerceAtLeast(0f)
        return width * height / center.area
    }
}
