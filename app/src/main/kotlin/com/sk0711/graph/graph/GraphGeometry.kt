package com.sk0711.graph.graph

object GraphGeometry {

    data class Segment(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val zone: Int,
    )

    data class Frame(
        val segments: List<Segment>,
        val widthPx: Int,
        val heightPx: Int,
        val yMin: Float,
        val yMax: Float,
    )

    fun compute(
        samples: List<Sample>,
        widthPx: Int,
        heightPx: Int,
        timeWindowSec: Int?,
        nowMs: Long,
    ): Frame {
        if (samples.isEmpty() || widthPx <= 0 || heightPx <= 0) {
            return Frame(emptyList(), widthPx, heightPx, 0f, 0f)
        }

        val windowStart: Long
        val windowMs: Long
        if (timeWindowSec == null) {
            windowStart = samples.first().timestampMs
            val span = samples.last().timestampMs - windowStart
            windowMs = if (span > 0) span else 1L
        } else {
            windowMs = timeWindowSec * 1000L
            windowStart = nowMs - windowMs
        }

        val anchorIdx = samples.indexOfLast { it.timestampMs < windowStart }
        val visible: List<Sample> = when {
            anchorIdx < 0 -> samples
            anchorIdx == samples.lastIndex -> listOf(samples[anchorIdx])
            else -> samples.subList(anchorIdx, samples.size)
        }

        if (visible.size < 2) {
            val v = visible.firstOrNull()?.value ?: 0f
            return Frame(emptyList(), widthPx, heightPx, v, v)
        }

        var vMin = visible.first().value
        var vMax = visible.first().value
        for (s in visible) {
            if (s.value < vMin) vMin = s.value
            if (s.value > vMax) vMax = s.value
        }
        val range = vMax - vMin
        val pad = if (range > 0f) range * 0.05f else maxOf(vMax * 0.05f, 1f)
        val yMin = vMin - pad
        val yMax = vMax + pad
        val ySpan = yMax - yMin

        val w = widthPx.toFloat()
        val h = heightPx.toFloat()

        fun xAt(tMs: Long): Float = (tMs - windowStart).toFloat() / windowMs.toFloat() * w
        fun yAt(v: Float): Float = h - ((v - yMin) / ySpan * h)

        val segments = ArrayList<Segment>(visible.size - 1)
        for (i in 1 until visible.size) {
            val a = visible[i - 1]
            val b = visible[i]
            segments.add(
                Segment(
                    x0 = xAt(a.timestampMs),
                    y0 = yAt(a.value),
                    x1 = xAt(b.timestampMs),
                    y1 = yAt(b.value),
                    zone = b.zone,
                ),
            )
        }
        return Frame(segments, widthPx, heightPx, yMin, yMax)
    }
}
