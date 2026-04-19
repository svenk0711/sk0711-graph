package com.sk0711.graph.graph

class DataBuffer(private val capacitySeconds: Int = 7200) {

    private val samples = ArrayDeque<Sample>()

    fun add(s: Sample) {
        samples.addLast(s)
        val cutoff = s.timestampMs - capacitySeconds * 1000L
        while (samples.isNotEmpty() && samples.first().timestampMs < cutoff) {
            samples.removeFirst()
        }
    }

    fun snapshot(): List<Sample> = samples.toList()

    fun size(): Int = samples.size

    fun clear() = samples.clear()
}
