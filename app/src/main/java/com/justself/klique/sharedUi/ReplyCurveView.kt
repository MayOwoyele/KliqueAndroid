package com.justself.klique.sharedUi

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.justself.klique.R

class ReplyCurveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = getSafeColor(context)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        val startX = 0f
        val startY = height.toFloat()

        val endX = width.toFloat()
        val endY = 0f

        path.reset()
        path.moveTo(startX, startY)

        // Cubic Bézier curve: (start) -> control1 -> control2 -> (end)
        path.cubicTo(
            width * 0.25f, height.toFloat(),   // Control Point 1 (pulls curve up)
            width * 0.75f, 0f,                // Control Point 2 (guides it flat)
            endX, endY                        // End point
        )

        canvas.drawPath(path, paint)
    }

    private fun getSafeColor(context: Context): Int {
        return if (isInEditMode) {
            Color.LTGRAY // Editor-safe fallback color
        } else {
            try {
                ContextCompat.getColor(context, R.color.primary)
            } catch (e: Exception) {
                Color.LTGRAY
            }
        }
    }
}