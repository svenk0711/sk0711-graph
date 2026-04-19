package com.sk0711.graph.graph

object ZoneColors {
    val HR = intArrayOf(
        0xFF5588CC.toInt(),
        0xFF44AA66.toInt(),
        0xFFDDBB22.toInt(),
        0xFFEE7722.toInt(),
        0xFFDD3333.toInt(),
    )

    val POWER = intArrayOf(
        0xFF888888.toInt(),
        0xFF5588CC.toInt(),
        0xFF44AA66.toInt(),
        0xFFDDBB22.toInt(),
        0xFFEE7722.toInt(),
        0xFFDD3333.toInt(),
        0xFFAA44CC.toInt(),
    )

    const val FALLBACK = 0xFF999999.toInt()

    fun hr(zone: Int): Int = pick(HR, zone)

    fun power(zone: Int): Int = pick(POWER, zone)

    private fun pick(palette: IntArray, zone: Int): Int {
        if (zone < 1) return FALLBACK
        val idx = (zone - 1).coerceAtMost(palette.size - 1)
        return palette[idx]
    }
}
