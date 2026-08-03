package com.example

import com.example.data.model.SoundGenome
import com.example.data.model.SoundType
import org.junit.Assert.*
import org.junit.Test

class MacSenseUnitTest {

    @Test
    fun testSoundGenomeBreedingFormula() {
        val parentA = SoundGenome(
            id = "parent_a",
            name = "Parent Alpha",
            soundType = SoundType.SUB_808,
            mass = 0.80f,
            radiance = 0.40f,
            entropy = 0.20f,
            curvature = 0.70f,
            chrom1 = 50,
            chrom2 = 1000,
            chrom3 = 80,
            chrom4 = 90,
            chrom5 = 40,
            generation = 1
        )

        val parentB = SoundGenome(
            id = "parent_b",
            name = "Parent Beta",
            soundType = SoundType.SYNTH,
            mass = 0.40f,
            radiance = 0.90f,
            entropy = 0.30f,
            curvature = 0.50f,
            chrom1 = 440,
            chrom2 = 500,
            chrom3 = 60,
            chrom4 = 70,
            chrom5 = 20,
            generation = 2
        )

        val child = SoundGenome.breed(parentA, parentB, weightA = 0.5f, mutationFactor = 0.05f)

        assertNotNull(child)
        assertEquals(3, child.generation) // max(1, 2) + 1 = 3
        assertTrue("Child mass should be bounded between 0.05 and 1.0", child.mass in 0.05f..1.0f)
        assertTrue("Child radiance should be bounded between 0.05 and 1.0", child.radiance in 0.05f..1.0f)
        assertEquals(parentA.id, child.parentAId)
        assertEquals(parentB.id, child.parentBId)
    }

    @Test
    fun testLazarusResurrectionScarCalculation() {
        val extinct = SoundGenome(
            id = "extinct_1",
            name = "Dark Void",
            soundType = SoundType.FX,
            mass = 0.95f,
            radiance = 0.10f,
            entropy = 0.90f,
            curvature = 0.99f,
            chrom1 = 100,
            chrom2 = 1200,
            chrom3 = 90,
            chrom4 = 90,
            chrom5 = 80,
            isExtinct = true,
            deathTimestamp = System.currentTimeMillis() - (3600000 * 24) // 24 hours ago
        )

        val resurrected = SoundGenome.resurrect(extinct)

        assertFalse(resurrected.isExtinct)
        assertTrue("Scar magnitude should be greater than 0", resurrected.scarMagnitude > 0f)
        assertTrue("Scar magnitude should be capped at 0.35", resurrected.scarMagnitude <= 0.35f)
        assertTrue("Entropy should increase due to resurrection scar", resurrected.entropy >= extinct.entropy)
    }

    @Test
    fun testWrightHeterozygosityCalculation() {
        val genomes = listOf(
            SoundGenome("g1", "G1", SoundType.KICK, 0.9f, 0.2f, 0.1f, 0.8f, 90, 200, 90, 50, 10),
            SoundGenome("g2", "G2", SoundType.SNARE, 0.3f, 0.9f, 0.4f, 0.5f, 250, 300, 70, 80, 20),
            SoundGenome("g3", "G3", SoundType.HIHAT, 0.1f, 0.95f, 0.2f, 0.3f, 8000, 100, 90, 40, 10)
        )

        val heterozygosity = SoundGenome.calculateHeterozygosity(genomes)
        assertTrue("Heterozygosity should be positive and bounded", heterozygosity in 0.1f..0.98f)
    }
}
