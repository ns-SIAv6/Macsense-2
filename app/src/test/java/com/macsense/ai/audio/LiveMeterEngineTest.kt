package com.macsense.ai.audio

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class LiveMeterEngineTest {

    private fun sineChunk(sampleRate: Int, freqHz: Double, count: Int, amplitude: Double, startPhase: Double): Pair<DoubleArray, Double> {
        val chunk = DoubleArray(count)
        var phase = startPhase
        val step = 2.0 * Math.PI * freqHz / sampleRate
        for (i in 0 until count) {
            chunk[i] = amplitude * sin(phase)
            phase += step
        }
        return Pair(chunk, phase)
    }

    @Test
    fun `ingest of loud sine signal raises peak level above silence`() {
        val engine = LiveMeterEngine(sampleRate = 44100, fftSize = 1024, spectrumBands = 32)
        var phase = 0.0
        repeat(20) {
            val (chunk, nextPhase) = sineChunk(44100, 440.0, 512, amplitude = 0.8, startPhase = phase)
            phase = nextPhase
            engine.ingest(chunk)
        }

        assertTrue("expected peak above -60dB silence floor, got ${engine.latestPeakDbL}", engine.latestPeakDbL > -20f)
        assertTrue("expected non-silent spectrum bins", engine.latestSpectrumDb.any { it > -80f })
    }

    @Test
    fun `ingest below fft window size does not crash and stays silent`() {
        val engine = LiveMeterEngine(sampleRate = 44100, fftSize = 1024, spectrumBands = 32)
        engine.ingest(DoubleArray(100) { 0.5 })

        assertTrue(engine.latestSpectrumDb.all { it <= -80f })
    }

    @Test
    fun `stop resets meters to silence floor`() {
        val engine = LiveMeterEngine(sampleRate = 44100, fftSize = 1024, spectrumBands = 32)
        val (chunk, _) = sineChunk(44100, 440.0, 2048, amplitude = 0.8, startPhase = 0.0)
        engine.ingest(chunk)
        assertTrue(engine.latestPeakDbL > -60f)

        engine.stop()

        assertTrue(engine.latestPeakDbL <= -60f)
        assertTrue(engine.latestSpectrumDb.all { it <= -80f })
    }
}
