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
        // Return a new array instead of mutating
        val result = pcm.copyOf()
        return result
    }
}
