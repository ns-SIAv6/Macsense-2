package com.macsense.ai.mastering

import com.macsense.ai.dsp.LoudnessMeter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class ABComparisonPlayerTest {

    @Test
    fun testProcessMastered_appliesGainAndLimit() {
        val sampleRate = 44100
        val size = sampleRate
        val left = DoubleArray(size)
        val right = DoubleArray(size)
        var phase = 0.0
        for (i in 0 until size) {
            val sample = sin(phase) * 0.5
            left[i] = sample
            right[i] = sample
            phase += 2.0 * Math.PI * 440.0 / sampleRate
        }
        val channels = arrayOf(left, right)

        // Process with large boost to trigger limiting
        val mastered = ABComparisonPlayer.processMastered(
            channels = channels,
            eqLow = 0f,
            eqMid = 0f,
            eqHigh = 0f,
            limiterThreshold = -12.0f, // large make-up gain (+12 dB)
            compressorThreshold = -12.0f,
            ceilingDbtp = -1.0,
            levelMatch = false,
            sampleRate = sampleRate
        )

        // Verify peaks do not exceed ceiling factor (-1 dBTP = ~0.89)
        val maxPeak = mastered[0].maxOf { Math.abs(it) }
        assertTrue("Peak should not exceed ceiling factor", maxPeak <= 0.9)
    }

    @Test
    fun testProcessMastered_levelMatchLoudness() {
        val sampleRate = 44100
        val size = sampleRate
        val left = DoubleArray(size)
        val right = DoubleArray(size)
        var phase = 0.0
        for (i in 0 until size) {
            val sample = sin(phase) * 0.4
            left[i] = sample
            right[i] = sample
            phase += 2.0 * Math.PI * 440.0 / sampleRate
        }
        val channels = arrayOf(left, right)

        val originalLufs = LoudnessMeter.integratedLufs(channels, sampleRate)

        // Process with level matching enabled
        val masteredLevelMatched = ABComparisonPlayer.processMastered(
            channels = channels,
            eqLow = 2f,
            eqMid = 1f,
            eqHigh = 2f,
            limiterThreshold = -6.0f, // boost loudness
            compressorThreshold = -12.0f,
            ceilingDbtp = -1.0,
            levelMatch = true,
            sampleRate = sampleRate
        )

        val masteredLufs = LoudnessMeter.integratedLufs(masteredLevelMatched, sampleRate)

        // Assert that the mastered signal's loudness with level-matching matches original mix loudness
        assertEquals("Loudness with level-matching should match original mix", originalLufs, masteredLufs, 0.1)
    }
}
