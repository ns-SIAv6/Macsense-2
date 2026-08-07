package com.macsense.ai.export

import com.macsense.ai.audio.SoundGenome
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ExportFactoryTest {

    private fun makeTestGenome() = SoundGenome(
        sourceId = "test-take-001", transient = 0.7, harmonicity = 0.5,
        brightness = 0.6, dynamics = 0.4, stereoWidth = 0.0,
        confidence = 0.8, parents = listOf("parent-a", "parent-b")
    )

    @Test
    fun `createBatch generates all 10 export formats`() {
        val jobs = ExportFactory.createBatch("proj-1", "Test Track", "take-1", makeTestGenome(), 180.0)
        assertEquals(10, jobs.size)
        val formats = jobs.map { it.format }.toSet()
        assertTrue(formats.contains(ExportFormat.FULL_MIX))
        assertTrue(formats.contains(ExportFormat.INSTRUMENTAL))
        assertTrue(formats.contains(ExportFormat.ACAPELLA))
        assertTrue(formats.contains(ExportFormat.STEMS_ZIP))
        assertTrue(formats.contains(ExportFormat.TIKTOK_15S))
        assertTrue(formats.contains(ExportFormat.INSTAGRAM_30S))
        assertTrue(formats.contains(ExportFormat.SLOWED_REVERB))
        assertTrue(formats.contains(ExportFormat.SPED_UP))
        assertTrue(formats.contains(ExportFormat.AAC_320))
        assertTrue(formats.contains(ExportFormat.MP3_320))
    }

    @Test
    fun `createBatch attaches Sound DNA to genome-shareable formats`() {
        val jobs = ExportFactory.createBatch("proj-1", "DNA Track", "take-1", makeTestGenome(), 180.0)
        val fullMix = jobs.find { it.format == ExportFormat.FULL_MIX }!!
        assertNotNull(fullMix.soundDna)
        assertTrue(fullMix.soundDna!!.contains("MACSENSE_DNA_V1"))
        val tiktok = jobs.find { it.format == ExportFormat.TIKTOK_15S }!!
        assertNotNull(tiktok.soundDna)
        val slowed = jobs.find { it.format == ExportFormat.SLOWED_REVERB }!!
        assertNotNull(slowed.soundDna)
    }

    @Test
    fun `createBatch without genome produces no Sound DNA`() {
        val jobs = ExportFactory.createBatch("proj-1", "No DNA Track", "take-1", null, 180.0)
        val fullMix = jobs.find { it.format == ExportFormat.FULL_MIX }!!
        assertNull(fullMix.soundDna)
    }

    @Test
    fun `createBatch sanitizes project name for file output`() {
        val jobs = ExportFactory.createBatch("proj-1", "Track: Special / Name?", "take-1", trackDurationSeconds = 120.0)
        val fullMix = jobs.find { it.format == ExportFormat.FULL_MIX }!!
        assertFalse(fullMix.outputFileName.contains(":"))
        assertFalse(fullMix.outputFileName.contains("/"))
        assertFalse(fullMix.outputFileName.contains("?"))
        assertTrue(fullMix.outputFileName.endsWith("_full_mix.wav"))
    }

    @Test
    fun `createBatch sets time constraints on social media formats`() {
        val jobs = ExportFactory.createBatch("proj-1", "Test", "take-1", trackDurationSeconds = 180.0)
        val tiktok = jobs.find { it.format == ExportFormat.TIKTOK_15S }!!
        assertNotNull(tiktok.timeConstraint)
        assertEquals(15.0, tiktok.timeConstraint!!.durationSeconds, 0.01)
        val instagram = jobs.find { it.format == ExportFormat.INSTAGRAM_30S }!!
        assertNotNull(instagram.timeConstraint)
        assertEquals(30.0, instagram.timeConstraint!!.durationSeconds, 0.01)
    }

    @Test
    fun `createBatch sets tempo modulation on trend formats`() {
        val jobs = ExportFactory.createBatch("proj-1", "Test", "take-1", trackDurationSeconds = 180.0)
        val slowed = jobs.find { it.format == ExportFormat.SLOWED_REVERB }!!
        assertNotNull(slowed.tempoModulation)
        assertEquals(0.85, slowed.tempoModulation!!.speedFactor, 0.001)
        assertTrue(slowed.tempoModulation!!.reverbAmount > 0)
        val spedUp = jobs.find { it.format == ExportFormat.SPED_UP }!!
        assertNotNull(spedUp.tempoModulation)
        assertEquals(1.25, spedUp.tempoModulation!!.speedFactor, 0.001)
        assertTrue(spedUp.tempoModulation!!.preservePitch)
    }

    @Test
    fun `createBatch excludes vocal stems from instrumental`() {
        val jobs = ExportFactory.createBatch("proj-1", "Test", "take-1", trackDurationSeconds = 180.0)
        val instrumental = jobs.find { it.format == ExportFormat.INSTRUMENTAL }!!
        assertTrue(instrumental.excludeStems.contains("Vocal/Adlib"))
        val acapella = jobs.find { it.format == ExportFormat.ACAPELLA }!!
        assertTrue(acapella.includeStems?.contains("Vocal/Adlib") == true)
    }

    @Test
    fun `findMostEnergeticWindow returns hook position around 30 percent`() {
        val windowStart = ExportFactory.findMostEnergeticWindow(180.0)
        assertTrue(windowStart > 40.0)
        assertTrue(windowStart < 70.0)
    }

    @Test
    fun `applyTempoModulation slows down audio`() {
        val samples = DoubleArray(1000) { sin(it * 0.1) }
        val output = ExportFactory.applyTempoModulation(samples, 44100, TempoModulation(0.5, false))
        assertEquals(samples.size * 2, output.size, 10)
    }

    @Test
    fun `applyTempoModulation speeds up audio`() {
        val samples = DoubleArray(1000) { sin(it * 0.1) }
        val output = ExportFactory.applyTempoModulation(samples, 44100, TempoModulation(2.0, true))
        assertEquals(samples.size / 2, output.size, 10)
    }

    @Test
    fun `applyReverb adds tail to audio`() {
        val samples = DoubleArray(10000) { if (it < 100) 1.0 else 0.0 }
        val output = ExportFactory.applyReverb(samples, 44100, 0.8)
        val tailEnergy = output.drop(200).take(1000).sumOf { abs(it) }
        assertTrue("Reverb should produce a tail after input ends", tailEnergy > 0.0)
    }

    @Test
    fun `extractWindow returns correct duration with fades`() {
        val samples = DoubleArray(44100 * 5) { 0.5 }
        val window = ExportFactory.extractWindow(samples, 44100, TimeConstraint(1.0, 2.0))
        assertEquals(44100 * 2, window.size)
        assertTrue(abs(window[0]) < 0.1)
        assertTrue(abs(window[window.size / 2]) > 0.4)
    }

    @Test
    fun `extractWindow handles start beyond end of audio`() {
        val samples = DoubleArray(44100) { 0.5 }
        val window = ExportFactory.extractWindow(samples, 44100, TimeConstraint(10.0, 5.0))
        assertEquals(0, window.size)
    }
}
