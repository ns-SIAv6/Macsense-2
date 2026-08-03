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
    }

    @Test
    fun testT2_Minus6() {
        val s = SignalGenerator.sine(997.0, 1.0, 48000, -6.0)
        val tp = TruePeakMeter.measure(s)
    }
}
