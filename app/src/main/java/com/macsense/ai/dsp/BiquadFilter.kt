package com.macsense.ai.dsp

class BiquadFilter(
    private val b0: Double, private val b1: Double, private val b2: Double,
    private val a1: Double, private val a2: Double
) {
    private var z1 = 0.0
    private var z2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    fun reset() { z1 = 0.0; z2 = 0.0 }

    fun coefficients(): DoubleArray = doubleArrayOf(b0, b1, b2, 1.0, a1, a2)
}
