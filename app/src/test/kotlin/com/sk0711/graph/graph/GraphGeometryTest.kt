package com.sk0711.graph.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphGeometryTest {

    @Test
    fun emptySamplesProducesEmptyFrame() {
        val frame = GraphGeometry.compute(emptyList(), 200, 100, 60, 1000L)
        assertTrue(frame.segments.isEmpty())
    }

    @Test
    fun singleSampleHasNoSegments() {
        val frame = GraphGeometry.compute(
            listOf(Sample(1000L, 120f, 2)),
            200, 100, 60, 1000L,
        )
        assertTrue(frame.segments.isEmpty())
    }

    @Test
    fun zeroDimensionsReturnEmpty() {
        val frame = GraphGeometry.compute(
            listOf(Sample(0L, 100f, 1), Sample(1000L, 110f, 1)),
            0, 100, 60, 1000L,
        )
        assertTrue(frame.segments.isEmpty())
    }

    @Test
    fun scrollWindowPlacesOldestLeftNewestRight() {
        val samples = listOf(
            Sample(0L, 100f, 1),
            Sample(60_000L, 150f, 3),
        )
        val frame = GraphGeometry.compute(samples, 100, 100, 60, 60_000L)
        assertEquals(1, frame.segments.size)
        val seg = frame.segments[0]
        assertEquals(0f, seg.x0, 0.01f)
        assertEquals(100f, seg.x1, 0.01f)
    }

    @Test
    fun fullWindowSpansFromFirstToLastSample() {
        val samples = listOf(
            Sample(1000L, 100f, 1),
            Sample(3000L, 200f, 2),
            Sample(5000L, 300f, 3),
        )
        val frame = GraphGeometry.compute(samples, 100, 100, null, 10_000L)
        assertEquals(2, frame.segments.size)
        assertEquals(0f, frame.segments.first().x0, 0.01f)
        assertEquals(100f, frame.segments.last().x1, 0.01f)
    }

    @Test
    fun anchorBeforeWindowIsIncludedForContinuity() {
        val samples = listOf(
            Sample(-5_000L, 50f, 1),
            Sample(10_000L, 100f, 2),
            Sample(20_000L, 150f, 3),
        )
        val frame = GraphGeometry.compute(samples, 100, 100, 60, 60_000L)
        assertEquals(2, frame.segments.size)
        assertTrue("anchor x should be negative", frame.segments.first().x0 < 0)
    }

    @Test
    fun dynamicYPlacesMaxNearTopMinNearBottom() {
        val samples = listOf(
            Sample(0L, 100f, 1),
            Sample(30_000L, 200f, 2),
            Sample(60_000L, 100f, 1),
        )
        val frame = GraphGeometry.compute(samples, 100, 100, 60, 60_000L)
        val ys = frame.segments.flatMap { listOf(it.y0, it.y1) }
        assertTrue("max value should be near top", ys.min() < 10f)
        assertTrue("min value should be near bottom", ys.max() > 90f)
    }

    @Test
    fun segmentColoredByLaterPointZone() {
        val samples = listOf(
            Sample(0L, 100f, 1),
            Sample(60_000L, 150f, 3),
        )
        val frame = GraphGeometry.compute(samples, 100, 100, 60, 60_000L)
        assertEquals(3, frame.segments[0].zone)
    }

    @Test
    fun allSamplesSameValueStillProducesFiniteSegments() {
        val samples = listOf(
            Sample(0L, 120f, 2),
            Sample(30_000L, 120f, 2),
            Sample(60_000L, 120f, 2),
        )
        val frame = GraphGeometry.compute(samples, 100, 100, 60, 60_000L)
        assertEquals(2, frame.segments.size)
        for (seg in frame.segments) {
            assertTrue(seg.y0.isFinite() && seg.y1.isFinite())
        }
    }

    @Test
    fun samplesAllBeforeWindowReturnNoSegments() {
        val samples = listOf(
            Sample(-120_000L, 100f, 1),
            Sample(-90_000L, 110f, 1),
        )
        val frame = GraphGeometry.compute(samples, 100, 100, 60, 0L)
        assertTrue(frame.segments.isEmpty())
    }
}

class DataBufferTest {

    @Test
    fun dropsSamplesOlderThanCapacity() {
        val buf = DataBuffer(capacitySeconds = 60)
        buf.add(Sample(0L, 100f, 1))
        buf.add(Sample(30_000L, 110f, 1))
        buf.add(Sample(90_000L, 120f, 2))
        assertEquals(2, buf.size())
        assertEquals(30_000L, buf.snapshot().first().timestampMs)
    }

    @Test
    fun keepsSamplesWithinCapacity() {
        val buf = DataBuffer(capacitySeconds = 60)
        buf.add(Sample(0L, 100f, 1))
        buf.add(Sample(30_000L, 110f, 1))
        buf.add(Sample(60_000L, 120f, 2))
        assertEquals(3, buf.size())
    }
}

class ZoneColorsTest {

    @Test
    fun hrZonesReturnDistinctColors() {
        val colors = (1..5).map { ZoneColors.hr(it) }.toSet()
        assertEquals(5, colors.size)
    }

    @Test
    fun powerZonesReturnDistinctColors() {
        val colors = (1..7).map { ZoneColors.power(it) }.toSet()
        assertEquals(7, colors.size)
    }

    @Test
    fun zoneBeyondRangeClampsToLast() {
        assertEquals(ZoneColors.hr(5), ZoneColors.hr(99))
        assertEquals(ZoneColors.power(7), ZoneColors.power(99))
    }

    @Test
    fun zoneZeroOrBelowReturnsFallback() {
        assertEquals(ZoneColors.FALLBACK, ZoneColors.hr(0))
        assertEquals(ZoneColors.FALLBACK, ZoneColors.power(-1))
    }
}
