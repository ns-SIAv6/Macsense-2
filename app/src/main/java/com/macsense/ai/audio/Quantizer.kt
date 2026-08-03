package com.macsense.ai.audio

import kotlin.math.roundToInt

object Quantizer {
    fun quantize(onsetSec: Double, bpm: Double, division: Int, strength: Double): Double {
        if (bpm <= 0) return onsetSec
        val beatsPerSec = bpm / 60.0
        val beatLengthSec = 1.0 / beatsPerSec
        val gridStepSec = beatLengthSec / (division / 4.0)
        
        val closestGridIndex = (onsetSec / gridStepSec).roundToInt()
        val closestGridSec = closestGridIndex * gridStepSec
        
        val diff = closestGridSec - onsetSec
        val offset = diff * strength
        return offset
    }
}
