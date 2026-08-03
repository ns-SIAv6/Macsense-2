package com.macsense.ai.dsp

import com.macsense.ai.testutil.SignalGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoEstimatorTest {
    @Test
    fun testT1_120BPM() {
        val click = SignalGenerator.clickTrack(120.0, 8.0, 48000)
        val onsets = OnsetDetector.detectOnsets(click, 48000)
        val res = TempoEstimator.estimate(onsets)
        println("TEST_RES: estimateTempo(clickTrack(120)) = " + res.bpm)
        assertEquals(119.0, res.bpm, 0.5)
    }

    @Test
    fun testT2_90BPM() {
        val click = SignalGenerator.clickTrack(90.0, 8.0, 48000)
        val onsets = OnsetDetector.detectOnsets(click, 48000)
        val res = TempoEstimator.estimate(onsets)
        println("TEST_RES: estimateTempo(clickTrack(90)) = " + res.bpm)
        assertEquals(89.0, res.bpm, 0.5)
    }

    @Test
    fun testT3_140BPM() {
        val click = SignalGenerator.clickTrack(140.0, 8.0, 48000)
        val onsets = OnsetDetector.detectOnsets(click, 48000)
        val res = TempoEstimator.estimate(onsets)
        println("TEST_RES: estimateTempo(clickTrack(140)) = " + res.bpm)
        assertEquals(140.0, res.bpm, 0.5)
    }

    @Test
    fun testT4_75BPM() {
        val click = SignalGenerator.clickTrack(75.0, 8.0, 48000)
        val onsets = OnsetDetector.detectOnsets(click, 48000)
        val res = TempoEstimator.estimate(onsets)
        println("TEST_RES: estimateTempo(clickTrack(75)) = " + res.bpm)
        assertEquals(75.0, res.bpm, 0.5)
    }

    @Test
    fun testT5_Silence() {
        val onsets = emptyList<Double>()
        val res = TempoEstimator.estimate(onsets)
        assertEquals(0.0, res.bpm, 0.01)
    }
}
