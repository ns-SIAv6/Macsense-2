package com.macsense.ai.dsp

import kotlin.math.log10

object LoudnessMeter {
    fun integratedLufs(channels: Array<DoubleArray>, sampleRate: Int): Double {
        if (channels.isEmpty() || channels[0].isEmpty()) return Double.NEGATIVE_INFINITY
        val numChannels = channels.size
        
        val filters = Array(numChannels) { KWeighting(sampleRate) }
        val weights = DoubleArray(numChannels)
        for (i in 0 until numChannels) {
            weights[i] = if (numChannels == 2) 1.0 else {
                when (i) {
                    0, 1, 2 -> 1.0 // L, R, C
                    3, 4 -> 1.41   // Ls, Rs
                    else -> 1.0
                }
            }
        }
        
        val blockSize = (0.4 * sampleRate).toInt()
        val stepSize = (0.1 * sampleRate).toInt()
        val totalSamples = channels[0].size
        
        val filtered = Array(numChannels) { DoubleArray(totalSamples) }
        for (ch in 0 until numChannels) {
            val f = filters[ch]
            val src = channels[ch]
            val dst = filtered[ch]
            for (i in 0 until totalSamples) {
                dst[i] = f.process(src[i])
            }
        }
        
        val numBlocks = if (totalSamples >= blockSize) (totalSamples - blockSize) / stepSize + 1 else 0
        if (numBlocks == 0) return Double.NEGATIVE_INFINITY
        
        val blockEnergy = Array(numChannels) { DoubleArray(numBlocks) }
        
        for (b in 0 until numBlocks) {
            val start = b * stepSize
            for (ch in 0 until numChannels) {
                var sumSq = 0.0
                for (i in 0 until blockSize) {
                    val s = filtered[ch][start + i]
                    sumSq += s * s
                }
                blockEnergy[ch][b] = sumSq / blockSize
            }
        }
        
        val survivingBlocks = BooleanArray(numBlocks)
        var numSurviving = 0
        for (b in 0 until numBlocks) {
            var sumGZ = 0.0
            for (ch in 0 until numChannels) {
                sumGZ += weights[ch] * blockEnergy[ch][b]
            }
            if (sumGZ > 0) {
                val l = -0.691 + 10.0 * log10(sumGZ)
                if (l > -70.0) {
                    survivingBlocks[b] = true
                    numSurviving++
                }
            }
        }
        
        if (numSurviving == 0) return Double.NEGATIVE_INFINITY
        
        var absSum = 0.0
        for (ch in 0 until numChannels) {
            var sumZ = 0.0
            for (b in 0 until numBlocks) {
                if (survivingBlocks[b]) {
                    sumZ += blockEnergy[ch][b]
                }
            }
            absSum += weights[ch] * (sumZ / numSurviving)
        }
        
        if (absSum <= 0) return Double.NEGATIVE_INFINITY
        val gammaR = -0.691 + 10.0 * log10(absSum) - 10.0
        
        var finalBlocks = 0
        var finalSum = 0.0
        val finalMeanZ = DoubleArray(numChannels)
        
        for (b in 0 until numBlocks) {
            if (survivingBlocks[b]) {
                var sumGZ = 0.0
                for (ch in 0 until numChannels) {
                    sumGZ += weights[ch] * blockEnergy[ch][b]
                }
                if (sumGZ > 0) {
                    val l = -0.691 + 10.0 * log10(sumGZ)
                    if (l > gammaR) {
                        finalBlocks++
                        for (ch in 0 until numChannels) {
                            finalMeanZ[ch] += blockEnergy[ch][b]
                        }
                    }
                }
            }
        }
        
        if (finalBlocks == 0) return Double.NEGATIVE_INFINITY
        
        for (ch in 0 until numChannels) {
            finalSum += weights[ch] * (finalMeanZ[ch] / finalBlocks)
        }
        
        if (finalSum <= 0) return Double.NEGATIVE_INFINITY
        return -0.691 + 10.0 * log10(finalSum)
    }

    fun momentaryLufs(channels: Array<DoubleArray>, sampleRate: Int): DoubleArray {
        return measureWindow(channels, sampleRate, 0.4)
    }

    fun shortTermLufs(channels: Array<DoubleArray>, sampleRate: Int): DoubleArray {
        return measureWindow(channels, sampleRate, 3.0)
    }
    
    private fun measureWindow(channels: Array<DoubleArray>, sampleRate: Int, windowSec: Double): DoubleArray {
        if (channels.isEmpty() || channels[0].isEmpty()) return DoubleArray(0)
        val numChannels = channels.size
        
        val filters = Array(numChannels) { KWeighting(sampleRate) }
        val weights = DoubleArray(numChannels)
        for (i in 0 until numChannels) {
            weights[i] = if (numChannels == 2) 1.0 else {
                when (i) {
                    0, 1, 2 -> 1.0 // L, R, C
                    3, 4 -> 1.41   // Ls, Rs
                    else -> 1.0
                }
            }
        }
        
        val blockSize = (windowSec * sampleRate).toInt()
        val stepSize = (0.1 * sampleRate).toInt() // typical 100ms step for timeline
        val totalSamples = channels[0].size
        val numBlocks = if (totalSamples >= blockSize) (totalSamples - blockSize) / stepSize + 1 else 0
        if (numBlocks == 0) return DoubleArray(0)
        
        val out = DoubleArray(numBlocks)
        for (b in 0 until numBlocks) {
            val start = b * stepSize
            var sumGZ = 0.0
            for (ch in 0 until numChannels) {
                var sumSq = 0.0
                // For simplicity, we just filter everything ahead of time, but to match the previous structure
                // we can just reset filters per block for momentary? Actually momentary uses overlapping windows of continuously filtered signal.
                // It is better to filter the whole signal first.
            }
            out[b] = Double.NEGATIVE_INFINITY // default val for full implementation
        }
        // Properly implementing the momentary and short-term array output for the whole track:
        val filtered = Array(numChannels) { DoubleArray(totalSamples) }
        for (ch in 0 until numChannels) {
            val f = filters[ch]
            val src = channels[ch]
            val dst = filtered[ch]
            for (i in 0 until totalSamples) {
                dst[i] = f.process(src[i])
            }
        }
        for (b in 0 until numBlocks) {
            val start = b * stepSize
            var sumGZ = 0.0
            for (ch in 0 until numChannels) {
                var sumSq = 0.0
                for (i in 0 until blockSize) {
                    val s = filtered[ch][start + i]
                    sumSq += s * s
                }
                val z = sumSq / blockSize
                sumGZ += weights[ch] * z
            }
            if (sumGZ > 0) {
                out[b] = -0.691 + 10.0 * log10(sumGZ)
            } else {
                out[b] = Double.NEGATIVE_INFINITY
            }
        }
        return out
    }

    fun loudnessRange(channels: Array<DoubleArray>, sampleRate: Int): Double {
        return 0.0 // Stub for LRA
    }
}
