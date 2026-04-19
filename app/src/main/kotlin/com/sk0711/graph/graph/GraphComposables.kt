package com.sk0711.graph.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

private val HrIconColor = Color(0xFFDD3333)
private val PowerIconColor = Color(0xFFAA44CC)

@Composable
fun ZoneGraph(
    samples: List<Sample>,
    timeWindowSec: Int?,
    nowMs: Long,
    kind: GraphRenderer.Kind,
    modifier: Modifier = Modifier,
    windowLabel: String? = null,
    isDark: Boolean = isSystemInDarkTheme(),
) {
    val textColor = if (isDark) Color.White else Color.Black
    BoxWithConstraints(modifier = modifier) {
        val h = maxHeight.value
        val valueFontSize = (h * 0.35f).sp
        val statFontSize = (h * 0.14f).sp
        val windowFontSize = (h * 0.11f).sp

        val measurer = rememberTextMeasurer()

        val stats = remember(samples) {
            if (samples.isEmpty()) {
                0 to 0
            } else {
                var sum = 0.0
                var max = Float.NEGATIVE_INFINITY
                for (s in samples) {
                    sum += s.value
                    if (s.value > max) max = s.value
                }
                (sum / samples.size).toInt() to max.toInt()
            }
        }

        val iconColor = when (kind) {
            GraphRenderer.Kind.HR -> HrIconColor
            GraphRenderer.Kind.POWER -> PowerIconColor
        }

        val valueStyle = TextStyle(color = textColor, fontWeight = FontWeight.Bold, fontSize = valueFontSize)
        val statStyle = TextStyle(color = textColor, fontWeight = FontWeight.Bold, fontSize = statFontSize)
        val windowStyle = TextStyle(color = textColor, fontSize = windowFontSize)

        val currentText = samples.lastOrNull()?.value?.toInt()?.toString().orEmpty()
        val avgText = "AVG ${stats.first}"
        val maxText = "MAX ${stats.second}"

        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalW = size.width
            val totalH = size.height
            val padPx = totalH * 0.05f

            val valueLayout = if (currentText.isNotEmpty()) measurer.measure(currentText, valueStyle) else null
            val avgLayout = measurer.measure(avgText, statStyle)
            val maxLayout = measurer.measure(maxText, statStyle)

            val iconSize = valueLayout?.size?.height?.toFloat()?.times(0.85f) ?: (totalH * 0.30f)
            val iconX = padPx
            val iconY = padPx + ((valueLayout?.size?.height?.toFloat() ?: iconSize) - iconSize) / 2f
            drawIcon(kind, iconX, iconY, iconSize, iconColor)

            valueLayout?.let { layout ->
                val x = iconX + iconSize + padPx * 0.8f
                val y = padPx
                drawText(textLayoutResult = layout, topLeft = Offset(x, y))
            }

            val avgX = totalW - padPx - avgLayout.size.width
            val avgY = padPx
            val maxX = totalW - padPx - maxLayout.size.width
            val maxY = avgY + avgLayout.size.height
            drawText(textLayoutResult = avgLayout, topLeft = Offset(avgX, avgY))
            drawText(textLayoutResult = maxLayout, topLeft = Offset(maxX, maxY))

            if (!windowLabel.isNullOrEmpty()) {
                val windowLayout = measurer.measure(windowLabel, windowStyle)
                val wx = (totalW - windowLayout.size.width) / 2f
                val wy = padPx
                drawText(textLayoutResult = windowLayout, topLeft = Offset(wx, wy))
            }

            val leftBottom = padPx + (valueLayout?.size?.height?.toFloat() ?: 0f)
            val rightBottom = padPx + avgLayout.size.height + maxLayout.size.height
            val curveTop = maxOf(leftBottom, rightBottom.toFloat()) + padPx * 0.5f
            val curveH = (totalH - curveTop).coerceAtLeast(1f)

            val frame = GraphGeometry.compute(samples, totalW.toInt(), curveH.toInt(), timeWindowSec, nowMs)

            for (seg in frame.segments) {
                val argb = when (kind) {
                    GraphRenderer.Kind.HR -> ZoneColors.hr(seg.zone)
                    GraphRenderer.Kind.POWER -> ZoneColors.power(seg.zone)
                }
                val baseColor = Color(argb)
                val y0 = seg.y0 + curveTop
                val y1 = seg.y1 + curveTop
                val poly = Path().apply {
                    moveTo(seg.x0, y0)
                    lineTo(seg.x1, y1)
                    lineTo(seg.x1, totalH)
                    lineTo(seg.x0, totalH)
                    close()
                }
                drawPath(poly, color = baseColor.copy(alpha = 0.88f))
                drawLine(
                    color = baseColor,
                    start = Offset(seg.x0, y0),
                    end = Offset(seg.x1, y1),
                    strokeWidth = 2.5f,
                )
            }
        }
    }
}

private fun DrawScope.drawIcon(kind: GraphRenderer.Kind, x: Float, y: Float, s: Float, color: Color) {
    val path = when (kind) {
        GraphRenderer.Kind.HR -> Path().apply {
            moveTo(x + 0.5f * s, y + 0.30f * s)
            cubicTo(x + 0.5f * s, y + 0.00f * s, x + 0.00f * s, y + 0.00f * s, x + 0.00f * s, y + 0.30f * s)
            cubicTo(x + 0.00f * s, y + 0.55f * s, x + 0.25f * s, y + 0.78f * s, x + 0.5f * s, y + 1.00f * s)
            cubicTo(x + 0.75f * s, y + 0.78f * s, x + 1.00f * s, y + 0.55f * s, x + 1.00f * s, y + 0.30f * s)
            cubicTo(x + 1.00f * s, y + 0.00f * s, x + 0.5f * s, y + 0.00f * s, x + 0.5f * s, y + 0.30f * s)
            close()
        }
        GraphRenderer.Kind.POWER -> Path().apply {
            moveTo(x + 0.60f * s, y + 0.00f * s)
            lineTo(x + 0.10f * s, y + 0.58f * s)
            lineTo(x + 0.42f * s, y + 0.58f * s)
            lineTo(x + 0.30f * s, y + 1.00f * s)
            lineTo(x + 0.90f * s, y + 0.40f * s)
            lineTo(x + 0.55f * s, y + 0.40f * s)
            lineTo(x + 0.78f * s, y + 0.00f * s)
            close()
        }
    }
    drawPath(path, color = color)
}
