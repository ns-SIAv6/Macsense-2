package com.macsense.ai.dsp

import com.macsense.ai.testutil.SignalGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchDetectorTest {
    @Test
    fun testT1_440Hz() {
        val s = SignalGenerator.sine(440.0, 0.1, 48000, 0.0)
        val res = PitchDetector.detect(s, 48000)
        println("TEST_RES: detectPitch(440 Hz sine) = " + (res?.frequency ?: "null"))
        assertEquals(440.0, res?.frequency ?: 0.0, 0.1)
    }

    @Test
    fun testT2_220Hz() {
        val s = SignalGenerator.sine(220.0, 0.1, 48000, 0.0)
        val res = PitchDetector.detect(s, 48000)
        assertEquals(220.0, res?.frequency ?: 0.0, 0.1)
    }

    @Test
    fun testT3_LowE() {
        val s = SignalGenerator.sine(82.41, 0.2, 48000, 0.0)
        val res = PitchDetector.detect(s, 48000)
        println("TEST_RES: detectPitch(82.41 Hz) = " + (res?.frequency ?: "null"))
        assertEquals(82.41, res?.frequency ?: 0.0, 0.1)
    }

    @Test
    fun testT4_Noise() {
        val s = SignalGenerator.whiteNoise(0.1, 48000, 1L, 0.0)
        val res = PitchDetector.detect(s, 48000)
        assertTrue(res == null || res.confidence < 0.2)
    }
}
