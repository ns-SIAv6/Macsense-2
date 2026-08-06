package com.macsense.ai.mastering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class MasteringAnalyzerTest {

    private fun generateSineMix(freq: Double, amplitude: Double, sampleRate: Int): Array<DoubleArray> {
        val size = sampleRate
        val left = DoubleArray(size)
        val right = DoubleArray(size)
        var phase = 0.0
        for (i in 0 until size) {
            val sample = sin(phase) * amplitude
            left[i] = sample
            right[i] = sample
            phase += 2.0 * Math.PI * freq / sampleRate
        }
        return arrayOf(left, right)
    }

    @Test
    fun testAnalyzeMix_insufficientLoudness() {
        val sampleRate = 44100
        val channels = generateSineMix(440.0, 0.02, sampleRate) // very quiet
        val result = MasteringAnalyzer.analyzeMix(channels, sampleRate, TargetProfile.POP)

        val lufsIssue = result.detectedIssues.find { it.name == "Insufficient Loudness" }
        assertNotNull("Should detect insufficient loudness", lufsIssue)
        assertEquals(MasteringAnalyzer.Severity.WARNING, lufsIssue?.severity)
    }

    @Test
    fun testAnalyzeMix_clippingRisk() {
        val sampleRate = 44100
        val channels = generateSineMix(440.0, 1.2, sampleRate) // very loud
        val result = MasteringAnalyzer.analyzeMix(channels, sampleRate, TargetProfile.POP)

        val peakIssue = result.detectedIssues.find { it.name == "Clipping Risk" }
        assertNotNull("Should detect clipping risk", peakIssue)
        assertEquals(MasteringAnalyzer.Severity.CRITICAL, peakIssue?.severity)
    }

    @Test
    fun testAnalyzeMix_muddyLowMids() {
        val sampleRate = 44100
        val channels = generateSineMix(300.0, 0.5, sampleRate) // 300Hz is in low-mids
        val result = MasteringAnalyzer.analyzeMix(channels, sampleRate, TargetProfile.POP)

        val muddyIssue = result.detectedIssues.find { it.name == "Muddy Low-Mids" }
        assertNotNull("Should detect muddy low-mids", muddyIssue)
        assertEquals(MasteringAnalyzer.Severity.WARNING, muddyIssue?.severity)
    }

    @Test
    fun testAnalyzeMix_harshHighShelf() {
        val sampleRate = 44100
        val channels = generateSineMix(8000.0, 0.5, sampleRate) // 8000Hz is in harsh highs
        val result = MasteringAnalyzer.analyzeMix(channels, sampleRate, TargetProfile.POP)

        val harshIssue = result.detectedIssues.find { it.name == "Harsh High-Shelf" }
        assertNotNull("Should detect harsh highs", harshIssue)
        assertEquals(MasteringAnalyzer.Severity.WARNING, harshIssue?.severity)
    }

    private fun assertNotNull(message: String, value: Any?) {
        assertTrue(message, value != null)
    }
}
