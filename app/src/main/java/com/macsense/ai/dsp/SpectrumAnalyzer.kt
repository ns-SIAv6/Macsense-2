package com.macsense.ai.dsp

import kotlin.math.log10

object SpectrumAnalyzer {
    fun analyze(samples: DoubleArray, sampleRate: Int): SpectrumData {
        if (samples.isEmpty()) return SpectrumData(0.0, 0.0, 0.0, DoubleArray(0))
        val n = Integer.highestOneBit(samples.size)
        val trimmed = DoubleArray(n) { i -> samples[i] }
        val window = WindowFunctions.hann(n)
        val mag = Fft.magnitudeSpectrum(trimmed, window)
        
        var sumMag = 0.0
        var sumMagWeighted = 0.0
        var geometricMean = 0.0
        var arithmeticMean = 0.0
        var logSum = 0.0
        
        for (i in mag.indices) {
            val f = i * sampleRate.toDouble() / n
            sumMag += mag[i]
            sumMagWeighted += mag[i] * f
            logSum += log10(mag[i] + 1e-15)
            arithmeticMean += mag[i]
        }
        
        val centroid = if (sumMag > 0.0) sumMagWeighted / sumMag else 0.0
        arithmeticMean /= mag.size
        geometricMean = Math.pow(10.0, logSum / mag.size)
        
        val flatness = if (arithmeticMean > 0.0) geometricMean / arithmeticMean else 0.0
        
        var cumulative = 0.0
        var rolloff = 0.0
        for (i in mag.indices) {
            cumulative += mag[i]
            if (cumulative >= 0.85 * sumMag) {
                rolloff = i * sampleRate.toDouble() / n
                break
            }
        }
        
        return SpectrumData(centroid, rolloff, flatness, mag)
    }
}

data class SpectrumData(
    val centroid: Double,
    val rolloff: Double,
    val flatness: Double,
    val magnitudes: DoubleArray
)
