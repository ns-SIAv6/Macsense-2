package com.macsense.ai.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class GenomeGuidanceTest {

    private val genome = SoundGenome(
        sourceId = "s1",
        transient = 0.7,
        harmonicity = 0.4,
        brightness = 0.9,
        dynamics = 0.55,
        stereoWidth = 0.3,
        confidence = 0.8,
        parents = listOf("a", "b"),
    )

    @Test
    fun `genome maps to slider params`() {
        val p = GenomeGuidance.fromGenome(genome)
        assertEquals(0.9, p.brightness, 1e-9)
        assertEquals(0.7, p.density, 1e-9)
        assertEquals(0.55, p.rhythmicComplexity, 1e-9)
        assertEquals(0.6, p.harmonicTension, 1e-9)
    }

    @Test
    fun `round trip is lossless and preserves identity fields`() {
        val p = GenomeGuidance.fromGenome(genome)
        val back = GenomeGuidance.applyTo(genome, p)
        assertEquals(genome, back)
    }

    @Test
    fun `edited sliders write through to the genome traits`() {
        val edited = GenomeGuidance.applyTo(
            genome,
            GenomeGuidanceParams(brightness = 0.1, density = 0.2, rhythmicComplexity = 0.3, harmonicTension = 1.0)
        )
        assertEquals(0.1, edited.brightness, 1e-9)
        assertEquals(0.2, edited.transient, 1e-9)
        assertEquals(0.3, edited.dynamics, 1e-9)
        assertEquals(0.0, edited.harmonicity, 1e-9)
        // Ancestry untouched — sliders never rewrite lineage.
        assertEquals(listOf("a", "b"), edited.parents)
        assertEquals("s1", edited.sourceId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `out of range slider values are rejected`() {
        GenomeGuidanceParams(1.2, 0.0, 0.0, 0.0)
    }
}
