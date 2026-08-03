package com.macsense.ai.testutil

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

object SignalGenerator {
    fun sine(freq: Double, seconds: Double, sampleRate: Int, amplitudeDbFs: Double): DoubleArray {
        val numSamples = (seconds * sampleRate).toInt()
        val amp = Math.pow(10.0, amplitudeDbFs / 20.0) * Math.sqrt(2.0)
        val out = DoubleArray(numSamples)
        for (i in 0 until numSamples) {
            out[i] = amp * sin(2.0 * PI * freq * i / sampleRate)
        }
        return out
    }
    
    fun silence(seconds: Double, sampleRate: Int): DoubleArray {
        val numSamples = (seconds * sampleRate).toInt()
        return DoubleArray(numSamples)
    }
    
    fun whiteNoise(seconds: Double, sampleRate: Int, seed: Long, amplitudeDbFs: Double): DoubleArray {
        val numSamples = (seconds * sampleRate).toInt()
        val amp = Math.pow(10.0, amplitudeDbFs / 20.0)
        val out = DoubleArray(numSamples)
        val random = Random(seed)
        for (i in 0 until numSamples) {
            out[i] = amp * (random.nextDouble() * 2.0 - 1.0)
        }
        return out
    }
    
    fun clickTrack(bpm: Double, seconds: Double, sampleRate: Int): DoubleArray {
        val numSamples = (seconds * sampleRate).toInt()
        val out = DoubleArray(numSamples)
        if (bpm <= 0) return out
        val beatInterval = (sampleRate * 60.0 / bpm).toInt()
        for (i in 0 until numSamples step beatInterval) {
            for (j in 0 until minOf(10, numSamples - i)) {
                out[i + j] = 1.0 - j / 10.0
            }
        }
        return out
    }
    
    fun impulse(position: Int, length: Int): DoubleArray {
        val out = DoubleArray(length)
        if (position in out.indices) {
            out[position] = 1.0
        }
        return out
    }
    
    fun sweep(f0: Double, f1: Double, seconds: Double, sampleRate: Int): DoubleArray {
        val numSamples = (seconds * sampleRate).toInt()
        val out = DoubleArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = f0 + (f1 - f0) * t / seconds
            out[i] = sin(2.0 * PI * freq * t)
        }
        return out
    }
}
