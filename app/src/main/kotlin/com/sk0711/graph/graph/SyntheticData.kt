package com.sk0711.graph.graph

import kotlin.math.sin
import kotlin.random.Random

object SyntheticData {

    fun hrSamples(nowMs: Long = 600_000L, durationSec: Int = 600, seed: Long = 42L): List<Sample> {
        val rng = Random(seed)
        val zoneBoundaries = intArrayOf(120, 140, 160, 175, 190)
        return (0 until durationSec).map { sec ->
            val t = nowMs - (durationSec - sec) * 1000L
            val base = 110f + 40f * sin(sec * 0.025).toFloat() + 20f * sin(sec * 0.11).toFloat()
            val jitter = rng.nextFloat() * 6f - 3f
            val hr = (base + jitter).coerceIn(70f, 200f)
            Sample(t, hr, zoneOf(hr, zoneBoundaries))
        }
    }

    fun powerSamples(nowMs: Long = 600_000L, durationSec: Int = 600, seed: Long = 17L): List<Sample> {
        val rng = Random(seed)
        val zoneBoundaries = intArrayOf(150, 220, 270, 320, 380, 450, 1500)
        return (0 until durationSec).map { sec ->
            val t = nowMs - (durationSec - sec) * 1000L
            val surge = if (sec % 90 in 60..75) 150f else 0f
            val base = 200f + 80f * sin(sec * 0.04).toFloat() + surge
            val jitter = rng.nextFloat() * 40f - 20f
            val p = (base + jitter).coerceIn(0f, 600f)
            Sample(t, p, zoneOf(p, zoneBoundaries))
        }
    }

    private fun zoneOf(v: Float, upper: IntArray): Int {
        for (i in upper.indices) if (v <= upper[i]) return i + 1
        return upper.size
    }
}
