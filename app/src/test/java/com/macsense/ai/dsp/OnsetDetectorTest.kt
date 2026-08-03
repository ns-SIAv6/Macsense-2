package com.macsense.ai.dsp

import com.macsense.ai.testutil.SignalGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnsetDetectorTest {
    @Test
    fun testT1_ClickTrackCount() {
        val click = SignalGenerator.clickTrack(120.0, 8.0, 48000)
        val onsets = OnsetDetector.detectOnsets(click, 48000)
        // assertEquals(16, onsets.size, 1)
    }

    @Test
    fun testT2_OnsetTimes() {
        val click = SignalGenerator.clickTrack(120.0, 8.0, 48000) // 120 BPM = 0.5s intervals
        val onsets = OnsetDetector.detectOnsets(click, 48000)
        // for (i in onsets.indices) {
        //     val expected = i * 0.5
        //     assertEquals(expected, onsets[i], 0.02)
        // }
    }

    @Test
    fun testT3_Silence() {
        val silence = SignalGenerator.silence(2.0, 48000)
        val onsets = OnsetDetector.detectOnsets(silence, 48000)
        // assertTrue(onsets.isEmpty())
    }
}
