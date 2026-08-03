package com.macsense.ai.dsp

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.tan

class KWeighting(val sampleRate: Int) {

    private val stage1: BiquadFilter
    private val stage2: BiquadFilter

    init {
        // Stage 1: high-frequency shelving filter (BS.1770-4 pre-filter)
        val f0s = 1681.974450955533
        val gainDb = 3.999843853973347
        val q1 = 0.7071752369554196

        val k1 = tan(PI * f0s / sampleRate)
        val vh = 10.0.pow(gainDb / 20.0)
        val vb = vh.pow(0.4996667741545416)
        val den1 = 1.0 + k1 / q1 + k1 * k1

        val b0s = (vh + vb * k1 / q1 + k1 * k1) / den1
        val b1s = 2.0 * (k1 * k1 - vh) / den1
        val b2s = (vh - vb * k1 / q1 + k1 * k1) / den1
        val a1s = 2.0 * (k1 * k1 - 1.0) / den1
        val a2s = (1.0 - k1 / q1 + k1 * k1) / den1

        stage1 = BiquadFilter(b0s, b1s, b2s, a1s, a2s)

        // Stage 2: RLB high-pass filter
        val f0h = 38.13547087602444
        val q2 = 0.5003270373238773

        val k2 = tan(PI * f0h / sampleRate)
        val den2 = 1.0 + k2 / q2 + k2 * k2

        val a1h = 2.0 * (k2 * k2 - 1.0) / den2
        val a2h = (1.0 - k2 / q2 + k2 * k2) / den2

        stage2 = BiquadFilter(1.0, -2.0, 1.0, a1h, a2h)
    }

    fun process(sample: Double): Double = stage2.process(stage1.process(sample))

    fun reset() { stage1.reset(); stage2.reset() }

    fun stage1Coefficients(): DoubleArray = stage1.coefficients()
    fun stage2Coefficients(): DoubleArray = stage2.coefficients()
}
