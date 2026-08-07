package com.macsense.ai.api

import com.macsense.ai.dsp.DynamicsResult
import com.macsense.ai.dsp.TempoResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProactiveSuggestionEngineTest {

    @Test
    fun `suggests reducing gain when TruePeak exceeds minus 1 dBTP`() {
        val suggestions = ProactiveSuggestionEngine.analyze(truePeakDbtp = -0.5)

        assertEquals(1, suggestions.size)
        val suggestion = suggestions[0]
        assertEquals(Severity.WARNING, suggestion.severity)
        assertTrue(suggestion.title.contains("True Peak"))
        assertTrue(suggestion.suggestedAction.contains("makeup gain"))
    }

    @Test
    fun `triggers critical suggestion when TruePeak clips above 0 dBTP`() {
        val suggestions = ProactiveSuggestionEngine.analyze(truePeakDbtp = 0.5)

        assertEquals(1, suggestions.size)
        val suggestion = suggestions[0]
        assertEquals(Severity.CRITICAL, suggestion.severity)
        assertTrue(suggestion.title.contains("Clipping"))
    }

    @Test
    fun `no peak suggestion when TruePeak is safe`() {
        val suggestions = ProactiveSuggestionEngine.analyze(truePeakDbtp = -3.0)
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `suggests variation when dynamics analyzer detects low crest factor flat section`() {
        val flatDynamics = DynamicsResult(rmsDb = -10.0, crestFactorDb = 4.0, plr = 4.0)
        val suggestions = ProactiveSuggestionEngine.analyze(dynamicsResult = flatDynamics)

        assertEquals(1, suggestions.size)
        val suggestion = suggestions[0]
        assertEquals(Severity.WARNING, suggestion.severity)
        assertTrue(suggestion.title.contains("Over-Compressed"))
        assertTrue(suggestion.description.contains("4.0 dB"))
    }

    @Test
    fun `no dynamics suggestion when crest factor is healthy`() {
        val healthyDynamics = DynamicsResult(rmsDb = -16.0, crestFactorDb = 12.0, plr = 12.0)
        val suggestions = ProactiveSuggestionEngine.analyze(dynamicsResult = healthyDynamics)
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `detects excessive loudness for integrated LUFS above minus 8`() {
        val suggestions = ProactiveSuggestionEngine.analyze(integratedLufs = -6.0)

        assertEquals(1, suggestions.size)
        val suggestion = suggestions[0]
        assertEquals(Severity.WARNING, suggestion.severity)
        assertTrue(suggestion.title.contains("Loudness"))
    }

    @Test
    fun `detects low loudness for integrated LUFS below minus 24`() {
        val suggestions = ProactiveSuggestionEngine.analyze(integratedLufs = -28.0)

        assertEquals(1, suggestions.size)
        val suggestion = suggestions[0]
        assertEquals(Severity.INFO, suggestion.severity)
        assertTrue(suggestion.title.contains("Low Integrated Loudness"))
    }

    @Test
    fun `warns on uncertain tempo estimation`() {
        val lowConfTempo = TempoResult(bpm = 120.0, confidence = 0.2)
        val suggestions = ProactiveSuggestionEngine.analyze(tempoResult = lowConfTempo)

        assertEquals(1, suggestions.size)
        val suggestion = suggestions[0]
        assertEquals(Severity.INFO, suggestion.severity)
        assertTrue(suggestion.title.contains("Tempo"))
    }

    @Test
    fun `generates multiple suggestions when multiple DSP issues are detected`() {
        val flatDynamics = DynamicsResult(rmsDb = -8.0, crestFactorDb = 3.5, plr = 3.5)
        val suggestions = ProactiveSuggestionEngine.analyze(
            truePeakDbtp = 0.2,
            dynamicsResult = flatDynamics,
            integratedLufs = -6.5,
            tempoResult = TempoResult(bpm = 95.0, confidence = 0.15)
        )

        assertEquals(4, suggestions.size)
        assertTrue(suggestions.any { it.severity == Severity.CRITICAL })
        assertTrue(suggestions.any { it.severity == Severity.WARNING })
        assertTrue(suggestions.any { it.severity == Severity.INFO })
    }

    @Test
    fun `returns empty list for clean DSP data`() {
        val cleanData = DspAnalysisData(
            truePeakDbtp = -2.0,
            dynamicsResult = DynamicsResult(rmsDb = -14.0, crestFactorDb = 10.0, plr = 10.0),
            integratedLufs = -14.0,
            tempoResult = TempoResult(bpm = 120.0, confidence = 0.9)
        )
        val suggestions = ProactiveSuggestionEngine.analyze(cleanData)
        assertTrue(suggestions.isEmpty())
    }
}
