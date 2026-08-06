package com.macsense.ai.mastering

import com.macsense.ai.dsp.BiquadFilter
import com.macsense.ai.dsp.LoudnessMeter
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

object ABComparisonPlayer {

    private fun makeLowShelf(sampleRate: Double, frequency: Double, q: Double, gainDb: Double): BiquadFilter {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * Math.PI * frequency / sampleRate
        val cosW0 = Math.cos(w0)
        val sinW0 = Math.sin(w0)
        val alpha = sinW0 / (2.0 * q)
        val beta = 2.0 * a.pow(0.5) * alpha

        val b0 = a * ((a + 1.0) - (a - 1.0) * cosW0 + beta)
        val b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0)
        val b2 = a * ((a + 1.0) - (a - 1.0) * cosW0 - beta)
        val a0 = (a + 1.0) + (a - 1.0) * cosW0 + beta
        val a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW0)
        val a2 = (a + 1.0) + (a - 1.0) * cosW0 - beta

        return BiquadFilter(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    private fun makeHighShelf(sampleRate: Double, frequency: Double, q: Double, gainDb: Double): BiquadFilter {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * Math.PI * frequency / sampleRate
        val cosW0 = Math.cos(w0)
        val sinW0 = Math.sin(w0)
        val alpha = sinW0 / (2.0 * q)
        val beta = 2.0 * a.pow(0.5) * alpha

        val b0 = a * ((a + 1.0) + (a - 1.0) * cosW0 + beta)
        val b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0)
        val b2 = a * ((a + 1.0) - (a - 1.0) * cosW0 - beta)
        val a0 = (a + 1.0) - (a - 1.0) * cosW0 + beta
        val a1 = 2.0 * ((a - 1.0) + (a + 1.0) * cosW0)
        val a2 = (a + 1.0) - (a - 1.0) * cosW0 - beta

        return BiquadFilter(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    private fun makePeakingEQ(sampleRate: Double, frequency: Double, q: Double, gainDb: Double): BiquadFilter {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * Math.PI * frequency / sampleRate
        val cosW0 = Math.cos(w0)
        val sinW0 = Math.sin(w0)
        val alpha = sinW0 / (2.0 * q)

        val b0 = 1.0 + alpha * a
        val b1 = -2.0 * cosW0
        val b2 = 1.0 - alpha * a
        val a0 = 1.0 + alpha / a
        val a1 = -2.0 * cosW0
        val a2 = 1.0 - alpha / a

        return BiquadFilter(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    fun processMastered(
        channels: Array<DoubleArray>,
        eqLow: Float,
        eqMid: Float,
        eqHigh: Float,
        limiterThreshold: Float,
        compressorThreshold: Float,
        ceilingDbtp: Double,
        levelMatch: Boolean,
        sampleRate: Int
    ): Array<DoubleArray> {
        if (channels.isEmpty() || channels[0].isEmpty()) return channels
        val numChannels = channels.size
        val numSamples = channels[0].size
        val output = Array(numChannels) { DoubleArray(numSamples) }

        val lowFilters = Array(numChannels) { makeLowShelf(sampleRate.toDouble(), 150.0, 0.707, eqLow.toDouble()) }
        val midFilters = Array(numChannels) { makePeakingEQ(sampleRate.toDouble(), 1000.0, 1.0, eqMid.toDouble()) }
        val highFilters = Array(numChannels) { makeHighShelf(sampleRate.toDouble(), 8000.0, 0.707, eqHigh.toDouble()) }

        val compressorRatio = 4.0
        val compThresh = compressorThreshold.toDouble()
        val attackCoeff = 0.99
        val releaseCoeff = 0.999

        val limiterGainDb = -limiterThreshold.toDouble()
        val makeUpGain = 10.0.pow(limiterGainDb / 20.0)
        val ceilingFactor = 10.0.pow(ceilingDbtp / 20.0)

        for (ch in 0 until numChannels) {
            var env = 0.0
            val lowF = lowFilters[ch]
            val midF = midFilters[ch]
            val highF = highFilters[ch]
            val src = channels[ch]
            val dst = output[ch]

            for (i in 0 until numSamples) {
                var x = src[i]
                x = lowF.process(x)
                x = midF.process(x)
                x = highF.process(x)

                val absX = abs(x)
                env = if (absX > env) {
                    attackCoeff * env + (1.0 - attackCoeff) * absX
                } else {
                    releaseCoeff * env + (1.0 - releaseCoeff) * absX
                }

                val envDb = if (env > 1e-15) 20.0 * log10(env) else -120.0
                var compressorGain = 1.0
                if (envDb > compThresh) {
                    val grDb = (1.0 - 1.0 / compressorRatio) * (compThresh - envDb)
                    compressorGain = 10.0.pow(grDb / 20.0)
                }
                x *= compressorGain
                x *= makeUpGain

                if (abs(x) > ceilingFactor) {
                    x = if (x > 0) ceilingFactor else -ceilingFactor
                }

                dst[i] = x
            }
        }

        if (levelMatch) {
            val originalLufs = LoudnessMeter.integratedLufs(channels, sampleRate)
            val masteredLufs = LoudnessMeter.integratedLufs(output, sampleRate)
            if (!originalLufs.isInfinite() && !originalLufs.isNaN() &&
                !masteredLufs.isInfinite() && !masteredLufs.isNaN()
            ) {
                val diffDb = originalLufs - masteredLufs
                val matchScale = 10.0.pow(diffDb / 20.0)
                for (ch in 0 until numChannels) {
                    for (i in 0 until numSamples) {
                        output[ch][i] *= matchScale
                    }
                }
            }
        }

        return output
    }
}
