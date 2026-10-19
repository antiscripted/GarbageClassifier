package org.garbageclassifier.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import android.util.AttributeSet
import android.view.View
import org.garbageclassifier.core.SortingStatus

data class OverlayItem(val bounds: RectF, val label: String, val status: SortingStatus)

class DetectionOverlay(context: Context, attributes: AttributeSet?) : View(context, attributes) {
    private val density = resources.displayMetrics.density
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }
    private val labelBounds = RectF()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13f, resources.displayMetrics)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val dashed = DashPathEffect(floatArrayOf(6f * density, 5f * density), 0f)
    private var items = emptyList<OverlayItem>()

    fun show(items: List<OverlayItem>) {
        this.items = items
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (item in items) {
            val color = when (item.status) {
                SortingStatus.CORRECT -> Color.rgb(105, 111, 255)
                SortingStatus.WRONG -> Color.rgb(210, 80, 41)
                SortingStatus.UNCERTAIN -> Color.rgb(150, 154, 169)
            }
            stroke.color = color
            stroke.pathEffect = if (item.status == SortingStatus.UNCERTAIN) dashed else null
            canvas.drawRoundRect(item.bounds, 10f * density, 10f * density, stroke)
            val padding = 8f * density
            val label = fitLabel(item.label, width - 4 * padding)
            val textWidth = textPaint.measureText(label)
            val labelWidth = (textWidth + 2 * padding).coerceAtMost(width.toFloat())
            val labelHeight = textPaint.fontSpacing + 2 * padding
            val left = item.bounds.left.coerceIn(0f, (width - labelWidth).coerceAtLeast(0f))
            val top = (item.bounds.top - labelHeight - 4 * density)
                .coerceIn(0f, (height - labelHeight).coerceAtLeast(0f))
            fill.color = Color.rgb(26, 27, 40)
            fill.alpha = 235
            labelBounds.set(left, top, left + labelWidth, top + labelHeight)
            canvas.drawRoundRect(labelBounds, 8f * density, 8f * density, fill)
            canvas.drawText(label, left + padding, top + padding - textPaint.fontMetrics.top, textPaint)
        }
    }

    private fun fitLabel(label: String, available: Float): String {
        if (available <= 0 || textPaint.measureText(label) <= available) return label
        val count = textPaint.breakText(label, true, available, null)
        return label.take(count)
    }
}
