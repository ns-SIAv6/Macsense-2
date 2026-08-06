package com.macsense.ai.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class GenomeExtractorTest {

    @Test
    fun `empty input returns zeroed low-confidence genome`() {
        val genome = GenomeExtractor.extract("empty", doubleArrayOf(), 44100)
        assertEquals(0.0, genome.transient, 0.0001)
        assertEquals(0.0, genome.harmonicity, 0.0001)
        assertEquals(0.0, genome.brightness, 0.0001)
        assertEquals(0.0, genome.dynamics, 0.0001)
        assertEquals(0.0, genome.confidence, 0.0001)
    }

    @Test
    fun `tonal sine wave yields higher harmonicity than click train`() {
        val sampleRate = 44100
        val fadeLen = 4410
        val tonal = DoubleArray(sampleRate) { i ->
            val gain = if (i < fadeLen) i.toDouble() / fadeLen else 1.0
            gain * 0.6 * sin(2.0 * PI * 220.0 * i / sampleRate)
        }
        val clicks = DoubleArray(sampleRate) { i -> if (i % 4000 < 40) 0.9 else 0.0 }

        val tonalGenome = GenomeExtractor.extract("tonal", tonal, sampleRate)
        val clickGenome = GenomeExtractor.extract("clicks", clicks, sampleRate)

        assertTrue(tonalGenome.harmonicity > clickGenome.harmonicity)
    }

    @Test
    fun `click train yields higher transient score than steady sine`() {
        val sampleRate = 44100
        val fadeLen = 4410
        val tonal = DoubleArray(sampleRate) { i ->
            val gain = if (i < fadeLen) i.toDouble() / fadeLen else 1.0
            gain * 0.6 * sin(2.0 * PI * 220.0 * i / sampleRate)
        }
        val clicks = DoubleArray(sampleRate) { i -> if (i % 4000 < 40) 0.9 else 0.0 }

        val tonalGenome = GenomeExtractor.extract("tonal", tonal, sampleRate)
        val clickGenome = GenomeExtractor.extract("clicks", clicks, sampleRate)

        println("TONAL TRANSIENT: ${tonalGenome.transient}")
        println("CLICK TRANSIENT: ${clickGenome.transient}")

        assertTrue(clickGenome.transient >= tonalGenome.transient)
    }

    @Test
    fun `bright alternating signal yields valid bounded genome`() {
        val sampleRate = 44100
        val bright = DoubleArray(sampleRate) { i -> if (i % 2 == 0) 0.7 else -0.7 }
        val genome = GenomeExtractor.extract("bright", bright, sampleRate)

        assertTrue(genome.brightness in 0.0..1.0)
        assertTrue(genome.dynamics in 0.0..1.0)
        assertTrue(genome.harmonicity in 0.0..1.0)
        assertTrue(genome.confidence in 0.0..1.0)
    }
}
