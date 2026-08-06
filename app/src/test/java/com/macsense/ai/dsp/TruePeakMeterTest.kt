package com.macsense.ai.dsp

import com.macsense.ai.testutil.SignalGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TruePeakMeterTest {
    @Test
    fun testT1_FullScale() {
        val s = SignalGenerator.sine(997.0, 1.0, 48000, 0.0)
        val tp = TruePeakMeter.measure(s)
        println("TEST_RES: dBTP(full-scale 997 Hz sine) = " + tp)
        // A sine wave at 0 dBFS has a true peak around 3.01 dB because of RMS vs peak and inter-sample interpolation.
        assertTrue("Expected true peak in 2.5..3.5 range, got $tp", tp in 2.5..3.5)
    }

    @Test
    fun testT2_Minus6() {
        val s = SignalGenerator.sine(997.0, 1.0, 48000, -6.0)
        val tp = TruePeakMeter.measure(s)
        println("TEST_RES: dBTP(-6 dBFS sine) = " + tp)
        assertTrue("Expected true peak in -3.5..-2.5 range, got $tp", tp in -3.5..-2.5)
    }

    @Test
    fun testT3_Silence() {
        val s = SignalGenerator.silence(1.0, 48000)
        val tp = TruePeakMeter.measure(s)
        assertEquals(Double.NEGATIVE_INFINITY, tp, 0.0)
    }
}
