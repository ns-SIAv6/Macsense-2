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
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(division > 0) { "division must be positive" }
        require(strength in 0.0..1.0) { "strength must be between 0 and 1" }
        if (pcm.isEmpty() || onsets.isEmpty() || bpm <= 0.0 || strength == 0.0) return pcm.copyOf()

        // Move each transient by the requested fraction of its nearest grid offset.
        // Collision samples are summed and clamped rather than silently discarded.
        val result = DoubleArray(pcm.size)
        val onsetIndexes = onsets
            .map { (it * sampleRate).roundToInt() }
            .filter { it in pcm.indices }
            .toSet()
        for (index in pcm.indices) {
            val sourceIndex = if (index in onsetIndexes) {
                (index + quantizeOffset(index.toDouble() / sampleRate, bpm, division, strength) * sampleRate)
                    .roundToInt()
            } else {
                index
            }
            if (sourceIndex in result.indices) {
                result[sourceIndex] = (result[sourceIndex] + pcm[index]).coerceIn(-1.0, 1.0)
            }
        }
        return result
    }
}
