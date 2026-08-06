package com.macsense.ai.dsp

import kotlin.math.roundToInt

object Quantizer {
    fun quantizeOffset(onsetSec: Double, bpm: Double, division: Int, strength: Double): Double {
        if (bpm <= 0) return 0.0
        val beatsPerSec = bpm / 60.0
        val beatLengthSec = 1.0 / beatsPerSec
        val gridStepSec = beatLengthSec / (division / 4.0)
        
        val closestGridIndex = (onsetSec / gridStepSec).roundToInt()
        val closestGridSec = closestGridIndex * gridStepSec
        
        val diff = closestGridSec - onsetSec
        return diff * strength
    }
    
    fun applyQuantize(pcm: DoubleArray, onsets: DoubleArray, bpm: Double, division: Int, strength: Double, sampleRate: Int): DoubleArray {
        if (pcm.isEmpty() || onsets.isEmpty() || bpm <= 0.0) return pcm.copyOf()
        val result = pcm.copyOf()

        // Define region size around each transient to shift (e.g., 40ms before, 120ms after)
        val beforeSamples = (0.040 * sampleRate).toInt()
        val afterSamples = (0.120 * sampleRate).toInt()

        for (onsetSec in onsets) {
            val offsetSec = quantizeOffset(onsetSec, bpm, division, strength)
            val offsetSamples = (offsetSec * sampleRate).toInt()
            if (offsetSamples == 0) continue

            val centerIdx = (onsetSec * sampleRate).toInt()
            val startIdx = (centerIdx - beforeSamples).coerceIn(0, pcm.size - 1)
            val endIdx = (centerIdx + afterSamples).coerceIn(0, pcm.size - 1)
            val len = endIdx - startIdx
            if (len <= 0) continue

            // Temporary buffer for the shifted transient region
            val tempRegion = DoubleArray(len)
            for (i in 0 until len) {
                // Read from pcm with offset
                val srcIdx = (startIdx + i - offsetSamples).coerceIn(0, pcm.size - 1)
                tempRegion[i] = pcm[srcIdx]
            }

            // Write back to result with a smooth fade-in/fade-out at borders to avoid pops/clicks
            val fadeSamples = minOf(128, len / 4)
            for (i in 0 until len) {
                val destIdx = startIdx + i
                val originalVal = pcm[destIdx]
                val shiftedVal = tempRegion[i]

                val weight = when {
                    i < fadeSamples -> i.toDouble() / fadeSamples
                    i > len - fadeSamples -> (len - i).toDouble() / fadeSamples
                    else -> 1.0
                }
                result[destIdx] = (1.0 - weight) * originalVal + weight * shiftedVal
            }
        }
        return result
    }
}
