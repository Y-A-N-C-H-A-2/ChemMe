/**
 * A row of views that wraps onto the next line when it runs out of width. Used for chips,
 * because the standard LinearLayout cannot wrap and the app does not use the Material library.
 */
package com.example.chemme.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.example.chemme.R
import kotlin.math.max

class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    /** Space between chips, both sideways and between lines. */
    private val gap = (8 * resources.displayMetrics.density).toInt()

    /** True centres each line (the log questions); false starts lines at the left. */
    private val centered: Boolean

    init {
        val values = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout)
        centered = values.getBoolean(R.styleable.FlowLayout_centered, false)
        values.recycle()
    }

    /** Splits the visible children into lines that fit in maxWidth. */
    private fun buildLines(maxWidth: Int): List<List<View>> {
        val lines = mutableListOf<MutableList<View>>()
        var lineWidth = 0
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility == GONE) continue
            val needed = if (lineWidth == 0) child.measuredWidth else lineWidth + gap + child.measuredWidth
            if (lines.isEmpty() || (lineWidth > 0 && needed > maxWidth)) {
                lines.add(mutableListOf(child))
                lineWidth = child.measuredWidth
            } else {
                lines.last().add(child)
                lineWidth = needed
            }
        }
        return lines
    }

    private fun widthOf(line: List<View>): Int = line.sumOf { it.measuredWidth } + gap * (line.size - 1)

    /** Step 1 of drawing a ViewGroup: ask every child how big it wants to be, then report our own size. */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val available =
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) Int.MAX_VALUE
            else MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight

        val lines = buildLines(available)
        val widest = lines.maxOfOrNull { widthOf(it) } ?: 0
        val height = lines.sumOf { line -> line.maxOf { it.measuredHeight } } + gap * max(0, lines.size - 1)

        setMeasuredDimension(
            resolveSize(widest + paddingLeft + paddingRight, widthMeasureSpec),
            resolveSize(height + paddingTop + paddingBottom, heightMeasureSpec)
        )
    }

    /** Step 2: give every child its position, line by line. */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val available = right - left - paddingLeft - paddingRight
        var y = paddingTop
        for (line in buildLines(available)) {
            val lineHeight = line.maxOf { it.measuredHeight }
            var x = paddingLeft + if (centered) max(0, (available - widthOf(line)) / 2) else 0
            for (child in line) {
                val childTop = y + (lineHeight - child.measuredHeight) / 2
                child.layout(x, childTop, x + child.measuredWidth, childTop + child.measuredHeight)
                x += child.measuredWidth + gap
            }
            y += lineHeight + gap
        }
    }
}
