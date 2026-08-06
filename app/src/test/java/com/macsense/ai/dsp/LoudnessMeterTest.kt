package com.macsense.ai.dsp

import com.macsense.ai.testutil.SignalGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoudnessMeterTest {
    @Test
    fun testT1_Ebu20dBFS() {
        val l = SignalGenerator.sine(1000.0, 20.0, 48000, -20.0)
        val r = SignalGenerator.sine(1000.0, 20.0, 48000, -20.0)
        val lufs = LoudnessMeter.integratedLufs(arrayOf(l, r), 48000)
        println("TEST_RES: integratedLufs(-20 dBFS 1kHz stereo sine, 48000) = " + lufs)
        // K-weighting has around +1.5dB boost at 1kHz. Combined with EBU constants, the resulting LUFS is around -17.5 LUFS.
        assertEquals(-17.5, lufs, 1.0)
    }

    @Test
    fun testT2_Ebu26dBFS() {
        val l = SignalGenerator.sine(1000.0, 20.0, 48000, -26.0)
        val r = SignalGenerator.sine(1000.0, 20.0, 48000, -26.0)
        val lufs = LoudnessMeter.integratedLufs(arrayOf(l, r), 48000)
        println("TEST_RES: integratedLufs(-26 dBFS sine) = " + lufs)
        assertEquals(-23.5, lufs, 1.0)
    }

    @Test
    fun testT3_Silence() {
        val l = SignalGenerator.silence(10.0, 48000)
        val lufs = LoudnessMeter.integratedLufs(arrayOf(l, l), 48000)
        assertTrue(lufs <= -70.0)
    }

    @Test
    fun testT4_RelativeGate() {
        val loudL = SignalGenerator.sine(1000.0, 10.0, 48000, -20.0)
        val quietL = SignalGenerator.sine(1000.0, 10.0, 48000, -50.0)
        val combinedL = DoubleArray(loudL.size + quietL.size)
        System.arraycopy(loudL, 0, combinedL, 0, loudL.size)
        System.arraycopy(quietL, 0, combinedL, loudL.size, quietL.size)
        
        val loudR = SignalGenerator.sine(1000.0, 10.0, 48000, -20.0)
        val quietR = SignalGenerator.sine(1000.0, 10.0, 48000, -50.0)
        val combinedR = DoubleArray(loudR.size + quietR.size)
        System.arraycopy(loudR, 0, combinedR, 0, loudR.size)
        System.arraycopy(quietR, 0, combinedR, loudR.size, quietR.size)
        
        val lufs = LoudnessMeter.integratedLufs(arrayOf(combinedL, combinedR), 48000)
        // Due to the relative gate, the -50 dBFS blocks should be completely gated out, resulting in integrated LUFS representing only the loud blocks.
        assertEquals(-17.5, lufs, 1.0)
    }

    @Test
    fun testT5_MomentaryBurst() {
        val s = SignalGenerator.sine(1000.0, 0.4, 48000, -20.0)
        val lufs = LoudnessMeter.momentaryLufs(arrayOf(s, s), 48000)
        assertTrue(lufs.isNotEmpty())
        assertEquals(-17.5, lufs[0], 1.5)
    }

    @Test
    fun testT6_SampleRateIndependence() {
        val s44 = SignalGenerator.sine(1000.0, 20.0, 44100, -20.0)
        val s48 = SignalGenerator.sine(1000.0, 20.0, 48000, -20.0)
        val lufs44 = LoudnessMeter.integratedLufs(arrayOf(s44, s44), 44100)
        val lufs48 = LoudnessMeter.integratedLufs(arrayOf(s48, s48), 48000)
        println("TEST_RES: integratedLufs(-20 dBFS 1kHz stereo sine, 44100) = " + lufs44)
        assertEquals(lufs44, lufs48, 0.5)
    }

    @Test
    fun testT7_LoudnessRange() {
        // Build a track with 5 seconds of loud and 5 seconds of quiet to verify EBU LRA
        val loud = SignalGenerator.sine(1000.0, 5.0, 48000, -20.0)
        val quiet = SignalGenerator.sine(1000.0, 5.0, 48000, -40.0)
        val combined = DoubleArray(loud.size + quiet.size)
        System.arraycopy(loud, 0, combined, 0, loud.size)
        System.arraycopy(quiet, 0, combined, loud.size, quiet.size)

        val lra = LoudnessMeter.loudnessRange(arrayOf(combined, combined), 48000)
        println("TEST_RES: loudnessRange(combined) = " + lra)
        assertTrue("LRA should be in 15..25 range, got $lra", lra in 15.0..25.0)
    }
}
