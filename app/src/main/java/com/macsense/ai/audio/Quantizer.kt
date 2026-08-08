package com.macsense.ai.audio

import kotlin.math.roundToInt

object Quantizer {
    fun quantize(onsetSec: Double, bpm: Double, division: Int, strength: Double): Double {
        return com.macsense.ai.dsp.Quantizer.quantizeOffset(onsetSec, bpm, division, strength)
    }
}
