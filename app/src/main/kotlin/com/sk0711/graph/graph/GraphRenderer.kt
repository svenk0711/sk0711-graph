package com.sk0711.graph.graph

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface

object GraphRenderer {

    private const val LINE_WIDTH_PX = 2.5f
    private const val FILL_ALPHA = 0xE0
    private val HR_ICON = Color.parseColor("#DD3333")
    private val POWER_ICON = Color.parseColor("#AA44CC")

    enum class Kind { HR, POWER }

    fun render(
        samples: List<Sample>,
        avg: Int,
        max: Int,
        widthPx: Int,
        heightPx: Int,
        timeWindowSec: Int?,
        nowMs: Long,
        kind: Kind,
        isDark: Boolean,
        windowLabel: String? = null,
    ): Bitmap {
        val textColor = if (isDark) Color.WHITE else Color.BLACK
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val padPx = h * 0.05f
        val statTextSize = h * 0.14f
        val windowTextSize = h * 0.11f

        val statPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = Typeface.DEFAULT
            textSize = statTextSize
            style = Paint.Style.FILL
        }
        val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = Typeface.DEFAULT
            textSize = windowTextSize
            style = Paint.Style.FILL
        }

        val currentText = samples.lastOrNull()?.value?.toInt()?.toString().orEmpty()
        val avgText = "AVG $avg"
        val maxText = "MAX $max"

        val avgBounds = Rect().also { statPaint.getTextBounds(avgText, 0, avgText.length, it) }
        val maxBounds = Rect().also { statPaint.getTextBounds(maxText, 0, maxText.length, it) }
        val avgWidth = statPaint.measureText(avgText)
        val maxWidth = statPaint.measureText(maxText)
        val rightColWidth = maxOf(avgWidth, maxWidth)
        val rightColLeft = w - padPx - rightColWidth

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = Typeface.DEFAULT
            textSize = h * 0.35f
            style = Paint.Style.FILL
        }

        val minCenterGap = padPx * 1.2f
        val maxLeftColRight = rightColLeft - minCenterGap

        var valueWidth = 0f
        var valueHeight = 0f
        var iconSize = 0f
        if (currentText.isNotEmpty()) {
            valueWidth = valuePaint.measureText(currentText)
            val vb = Rect().also { valuePaint.getTextBounds(currentText, 0, currentText.length, it) }
            valueHeight = vb.height().toFloat()
            iconSize = valueHeight * 0.85f
            val leftUsed = padPx + iconSize + padPx * 0.8f + valueWidth
            if (leftUsed > maxLeftColRight) {
                val scale = (maxLeftColRight - padPx) / (iconSize + padPx * 0.8f + valueWidth)
                valuePaint.textSize *= scale
                valueWidth = valuePaint.measureText(currentText)
                valuePaint.getTextBounds(currentText, 0, currentText.length, vb)
                valueHeight = vb.height().toFloat()
                iconSize = valueHeight * 0.85f
            }
        }

        val iconX = padPx
        val iconTopAlign = padPx + (valueHeight - iconSize) / 2f
        drawIcon(canvas, kind, iconX, iconTopAlign, iconSize)

        val valueTextX = iconX + iconSize + padPx * 0.8f
        val valueBaseline = padPx + valueHeight
        if (currentText.isNotEmpty()) {
            canvas.drawText(currentText, valueTextX, valueBaseline, valuePaint)
        }

        val avgBaseline = padPx + avgBounds.height()
        val avgX = w - padPx - avgWidth
        canvas.drawText(avgText, avgX, avgBaseline, statPaint)

        val maxBaseline = avgBaseline + statPaint.fontSpacing
        val maxX = w - padPx - maxWidth
        canvas.drawText(maxText, maxX, maxBaseline, statPaint)

        if (!windowLabel.isNullOrEmpty()) {
            val leftColRight = valueTextX + valueWidth
            val centerLeft = leftColRight + padPx * 0.8f
            val centerRight = rightColLeft - padPx * 0.8f
            val available = centerRight - centerLeft
            if (available > 0f) {
                var labelWidth = windowPaint.measureText(windowLabel)
                if (labelWidth > available) {
                    windowPaint.textSize *= available / labelWidth
                    labelWidth = windowPaint.measureText(windowLabel)
                }
                if (windowPaint.textSize >= h * 0.06f) {
                    val windowBounds = Rect().also { windowPaint.getTextBounds(windowLabel, 0, windowLabel.length, it) }
                    val wx = centerLeft + (available - labelWidth) / 2f
                    val wy = padPx + windowBounds.height()
                    canvas.drawText(windowLabel, wx, wy, windowPaint)
                }
            }
        }

        val leftBottom = padPx + valueHeight
        val rightBottom = padPx + avgBounds.height() + statPaint.fontSpacing
        val curveTop = maxOf(leftBottom, rightBottom) + padPx * 0.5f
        val curveH = (h - curveTop).coerceAtLeast(1f)

        val frame = GraphGeometry.compute(samples, w, curveH.toInt(), timeWindowSec, nowMs)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = LINE_WIDTH_PX
            strokeCap = Paint.Cap.ROUND
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val path = Path()
        val totalH = h.toFloat()
        for (seg in frame.segments) {
            val baseColor = when (kind) {
                Kind.HR -> ZoneColors.hr(seg.zone)
                Kind.POWER -> ZoneColors.power(seg.zone)
            }
            fillPaint.color = (baseColor and 0x00FFFFFF) or (FILL_ALPHA shl 24)
            val y0 = seg.y0 + curveTop
            val y1 = seg.y1 + curveTop
            path.reset()
            path.moveTo(seg.x0, y0)
            path.lineTo(seg.x1, y1)
            path.lineTo(seg.x1, totalH)
            path.lineTo(seg.x0, totalH)
            path.close()
            canvas.drawPath(path, fillPaint)

            linePaint.color = baseColor
            canvas.drawLine(seg.x0, y0, seg.x1, y1, linePaint)
        }

        return bmp
    }

    private fun drawIcon(canvas: Canvas, kind: Kind, x: Float, y: Float, s: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (kind == Kind.HR) HR_ICON else POWER_ICON
        }
        val p = Path()
        when (kind) {
            Kind.HR -> {
                p.moveTo(x + 0.50f * s, y + 0.30f * s)
                p.cubicTo(x + 0.50f * s, y + 0.00f * s, x + 0.00f * s, y + 0.00f * s, x + 0.00f * s, y + 0.30f * s)
                p.cubicTo(x + 0.00f * s, y + 0.55f * s, x + 0.25f * s, y + 0.78f * s, x + 0.50f * s, y + 1.00f * s)
                p.cubicTo(x + 0.75f * s, y + 0.78f * s, x + 1.00f * s, y + 0.55f * s, x + 1.00f * s, y + 0.30f * s)
                p.cubicTo(x + 1.00f * s, y + 0.00f * s, x + 0.50f * s, y + 0.00f * s, x + 0.50f * s, y + 0.30f * s)
                p.close()
            }
            Kind.POWER -> {
                p.moveTo(x + 0.60f * s, y + 0.00f * s)
                p.lineTo(x + 0.10f * s, y + 0.58f * s)
                p.lineTo(x + 0.42f * s, y + 0.58f * s)
                p.lineTo(x + 0.30f * s, y + 1.00f * s)
                p.lineTo(x + 0.90f * s, y + 0.40f * s)
                p.lineTo(x + 0.55f * s, y + 0.40f * s)
                p.lineTo(x + 0.78f * s, y + 0.00f * s)
                p.close()
            }
        }
        canvas.drawPath(p, paint)
    }
}
