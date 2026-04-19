package com.sk0711.graph.graph

class PowerSmoother(private val windowSeconds: Int = 3) {

    private val samples = ArrayDeque<Pair<Long, Float>>()

    fun add(timestampMs: Long, value: Float): Float {
        samples.addLast(timestampMs to value)
        val cutoff = timestampMs - windowSeconds * 1000L
        while (samples.isNotEmpty() && samples.first().first < cutoff) {
            samples.removeFirst()
        }
        if (samples.isEmpty()) return value
        var sum = 0f
        for ((_, v) in samples) sum += v
        return sum / samples.size
    }
}
