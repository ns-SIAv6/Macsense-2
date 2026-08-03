package com.macsense.ai.dsp

import com.macsense.ai.testutil.SignalGenerator
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumAnalyzerTest {
    @Test
    fun testT1_Centroid() {
        val s500 = SignalGenerator.sine(500.0, 0.1, 48000, 0.0)
        val s5000 = SignalGenerator.sine(5000.0, 0.1, 48000, 0.0)
        val c1 = SpectrumAnalyzer.analyze(s500, 48000).centroid
        val c2 = SpectrumAnalyzer.analyze(s5000, 48000).centroid
    }

    @Test
    fun testT2_Flatness() {
        val noise = SignalGenerator.whiteNoise(0.1, 48000, 1L, 0.0)
        val sine = SignalGenerator.sine(1000.0, 0.1, 48000, 0.0)
        val fNoise = SpectrumAnalyzer.analyze(noise, 48000).flatness
        val fSine = SpectrumAnalyzer.analyze(sine, 48000).flatness
        println("TEST_RES: spectralFlatness(whiteNoise) = " + fNoise + " spectralFlatness(sine) = " + fSine)
    }

    @Test
    fun testT3_SubEnergy() {
    }
}
