package com.macsense.ai.dsp

import kotlin.math.sqrt
import kotlin.math.log10

object DynamicsAnalyzer {
    fun analyze(samples: DoubleArray): DynamicsResult {
        if (samples.isEmpty()) return DynamicsResult(-Double.MAX_VALUE, 0.0, 0.0)
        var sumSquares = 0.0
        var peak = 0.0
        for (s in samples) {
            sumSquares += s * s
            if (Math.abs(s) > peak) {
                peak = Math.abs(s)
            }
        }
        val rms = sqrt(sumSquares / samples.size)
        val rmsDb = if (rms > 0) 20.0 * log10(rms) else -Double.MAX_VALUE
        val peakDb = if (peak > 0) 20.0 * log10(peak) else -Double.MAX_VALUE
        val crestFactor = peakDb - rmsDb
        return DynamicsResult(rmsDb, crestFactor, crestFactor)
    }
}

data class DynamicsResult(val rmsDb: Double, val crestFactorDb: Double, val plr: Double)
