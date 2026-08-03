package com.macsense.ai.dsp

import kotlin.math.abs

object TempoEstimator {
    fun estimate(onsets: List<Double>): TempoResult {
        if (onsets.size < 2) return TempoResult(0.0, 0.0)
        
        val intervals = mutableListOf<Double>()
        for (i in 0 until onsets.size - 1) {
            val diff = onsets[i + 1] - onsets[i]
            if (diff in 0.2..1.5) { // 40 to 300 BPM
                intervals.add(diff)
            }
        }
        
        if (intervals.isEmpty()) return TempoResult(0.0, 0.0)
        
        val bins = DoubleArray(260) // 40 to 300 BPM
        for (interval in intervals) {
            val bpm = 60.0 / interval
            if (bpm in 40.0..300.0) {
                val bin = bpm.toInt() - 40
                if (bin in bins.indices) {
                    bins[bin] += 1.0
                }
            }
        }
        
        var maxBin = -1
        var maxVal = 0.0
        for (i in bins.indices) {
            if (bins[i] > maxVal) {
                maxVal = bins[i]
                maxBin = i
            }
        }
        
        if (maxBin == -1) return TempoResult(0.0, 0.0)
        
        val bpm = maxBin + 40.0
        val confidence = minOf(1.0, maxVal / intervals.size)
        return TempoResult(bpm, confidence)
    }
}

data class TempoResult(val bpm: Double, val confidence: Double)
