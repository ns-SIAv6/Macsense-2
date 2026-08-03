package com.macsense.ai.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin

object TruePeakMeter {
    private const val OS = 4
    private const val TAPS_PER_PHASE = 12
    private const val N = OS * TAPS_PER_PHASE   // 48

    private val phases: Array<DoubleArray> = buildPolyphase()

    private fun buildPolyphase(): Array<DoubleArray> {
        val h = DoubleArray(N)
        val center = (N - 1) / 2.0
        for (n in 0 until N) {
            val x = n - center
            val sinc = if (abs(x) < 1e-12) 1.0 / OS
                       else sin(PI * x / OS) / (PI * x)
            val hann = 0.5 - 0.5 * cos(2.0 * PI * n / (N - 1))
            h[n] = sinc * hann
        }
        var sum = 0.0
        for (v in h) sum += v
        for (n in 0 until N) h[n] = h[n] * OS / sum

        return Array(OS) { p -> DoubleArray(TAPS_PER_PHASE) { k -> h[k * OS + p] } }
    }

    fun measure(samples: DoubleArray): Double = measureDbtp(samples)

    fun measureDbtp(samples: DoubleArray): Double {
        if (samples.isEmpty()) return Double.NEGATIVE_INFINITY
        var peak = 0.0
        val hist = DoubleArray(TAPS_PER_PHASE)

        for (s in samples) {
            for (i in TAPS_PER_PHASE - 1 downTo 1) hist[i] = hist[i - 1]
            hist[0] = s

            for (p in 0 until OS) {
                var acc = 0.0
                val ph = phases[p]
                for (k in 0 until TAPS_PER_PHASE) acc += hist[k] * ph[k]
                val a = abs(acc)
                if (a > peak) peak = a
            }
        }
        if (peak <= 1e-15) return Double.NEGATIVE_INFINITY
        return 20.0 * log10(peak)
    }
    
    fun coefficients(): Array<DoubleArray> = phases
}
