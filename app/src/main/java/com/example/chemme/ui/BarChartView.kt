/**
 * A small bar chart drawn directly on a Canvas, used for sleep hours on the Trends screen.
 */
package com.example.chemme.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.chemme.R
import kotlin.math.max
import kotlin.math.min

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    /** One bar: the label under it and its value, or null for a day with no entry. */
    data class Bar(val label: String, val value: Double?)

    private var bars: List<Bar> = emptyList()
    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.bg_brand)
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.border_default)
    }
    private val valuePaint = textPaint(R.color.text_primary)
    private val labelPaint = textPaint(R.color.text_secondary)
    private val rect = RectF()

    private fun textPaint(colorId: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, colorId)
        textSize = 12 * scaledDensity
        textAlign = Paint.Align.CENTER
        // The font is not available in the layout editor preview.
        if (!isInEditMode) typeface = ResourcesCompat.getFont(context, R.font.nunito_semibold)
    }

    /** Replaces the chart's data and redraws it. */
    fun setBars(newBars: List<Bar>) {
        bars = newBars
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bars.isEmpty()) return

        val textHeight = 16 * density
        val chartTop = textHeight + 4 * density          // Room for the value above the tallest bar.
        val chartBottom = height - textHeight - 4 * density // Room for the label under the bars.
        val slot = width.toFloat() / bars.size
        val barWidth = min(28 * density, slot * 0.6f)
        // At least 8 hours tall, so a week of short nights does not look like a week of full bars.
        val top = max(8.0, bars.maxOf { it.value ?: 0.0 })
        val fewBars = bars.size <= 10

        bars.forEachIndexed { index, bar ->
            val centre = slot * index + slot / 2
            val value = bar.value
            if (value == null) {
                // No entry that day: a short grey stub instead of a bar.
                rect.set(centre - barWidth / 2, chartBottom - 4 * density, centre + barWidth / 2, chartBottom)
                canvas.drawRoundRect(rect, 2 * density, 2 * density, emptyPaint)
            } else {
                val barHeight = max(4 * density, ((value / top) * (chartBottom - chartTop)).toFloat())
                rect.set(centre - barWidth / 2, chartBottom - barHeight, centre + barWidth / 2, chartBottom)
                val radius = min(8 * density, barWidth / 2)
                canvas.drawRoundRect(rect, radius, radius, barPaint)
                // Values are written on the bars only when there is room (the 7-day view).
                if (fewBars) canvas.drawText(Format.number(value), centre, rect.top - 6 * density, valuePaint)
            }
            if (bar.label.isNotEmpty()) canvas.drawText(bar.label, centre, height - 4 * density, labelPaint)
        }
    }
}
