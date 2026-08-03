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
        assertEquals(-16.98, lufs, 0.1)
    }

    @Test
    fun testT2_Ebu26dBFS() {
        val l = SignalGenerator.sine(1000.0, 20.0, 48000, -26.0)
        val r = SignalGenerator.sine(1000.0, 20.0, 48000, -26.0)
        val lufs = LoudnessMeter.integratedLufs(arrayOf(l, r), 48000)
        println("TEST_RES: integratedLufs(-26 dBFS sine) = " + lufs)
        assertEquals(-22.98, lufs, 0.1)
    }

    @Test
    fun testT3_Silence() {
        val l = SignalGenerator.silence(10.0, 48000)
        val lufs = LoudnessMeter.integratedLufs(arrayOf(l, l), 48000)
        assertTrue(lufs <= -70.0)
    }

    @Test
    fun testT4_RelativeGate() {
        val loudL = SignalGenerator.sine(1000.0, 20.0, 48000, -20.0)
        val quietL = SignalGenerator.sine(1000.0, 20.0, 48000, -40.0)
        val combinedL = DoubleArray(loudL.size + quietL.size)
        System.arraycopy(loudL, 0, combinedL, 0, loudL.size)
        System.arraycopy(quietL, 0, combinedL, loudL.size, quietL.size)
        
        val loudR = SignalGenerator.sine(1000.0, 20.0, 48000, -20.0)
        val quietR = SignalGenerator.sine(1000.0, 20.0, 48000, -40.0)
        val combinedR = DoubleArray(loudR.size + quietR.size)
        System.arraycopy(loudR, 0, combinedR, 0, loudR.size)
        System.arraycopy(quietR, 0, combinedR, loudR.size, quietR.size)
        
        val lufs = LoudnessMeter.integratedLufs(arrayOf(combinedL, combinedR), 48000)
        assertEquals(-16.98, lufs, 0.2)
    }

    @Test
    fun testT5_MomentaryBurst() {
        val s = SignalGenerator.sine(1000.0, 0.4, 48000, -20.0)
        val lufs = LoudnessMeter.momentaryLufs(arrayOf(s, s), 48000)
        assertEquals(1, lufs.size)
        assertEquals(-16.98, lufs[0], 0.1)
    }

    @Test
    fun testT6_SampleRateIndependence() {
        val s44 = SignalGenerator.sine(1000.0, 20.0, 44100, -20.0)
        val s48 = SignalGenerator.sine(1000.0, 20.0, 48000, -20.0)
        val lufs44 = LoudnessMeter.integratedLufs(arrayOf(s44, s44), 44100)
        val lufs48 = LoudnessMeter.integratedLufs(arrayOf(s48, s48), 48000)
        println("TEST_RES: integratedLufs(-20 dBFS 1kHz stereo sine, 44100) = " + lufs44)
        assertEquals(lufs44, lufs48, 0.15)
    }
}
