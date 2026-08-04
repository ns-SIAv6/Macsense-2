package com.macsense.ai.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SoundBreederTest {

    private val parentA = SoundGenome(
        sourceId = "parentA",
        transient = 0.9,
        harmonicity = 0.9,
        brightness = 0.9,
        dynamics = 0.9,
        stereoWidth = 0.9,
        confidence = 0.8
    )

    private val parentB = SoundGenome(
        sourceId = "parentB",
        transient = 0.1,
        harmonicity = 0.1,
        brightness = 0.1,
        dynamics = 0.1,
        stereoWidth = 0.1,
        confidence = 0.6
    )

    @Test
    fun breedRandom_withZeroBias_alwaysInheritsFromParentA() {
        val breeder = SoundBreeder(Random(42))
        val child = breeder.breedRandom(parentA, parentB, traitBiasTowardsB = 0.0)

        assertEquals(parentA.transient, child.transient, 0.0)
        assertEquals(parentA.harmonicity, child.harmonicity, 0.0)
        assertEquals(parentA.brightness, child.brightness, 0.0)
        assertEquals(parentA.dynamics, child.dynamics, 0.0)
        assertEquals(parentA.stereoWidth, child.stereoWidth, 0.0)
    }

    @Test
    fun breedRandom_withFullBias_alwaysInheritsFromParentB() {
        val breeder = SoundBreeder(Random(42))
        val child = breeder.breedRandom(parentA, parentB, traitBiasTowardsB = 1.0)

        assertEquals(parentB.transient, child.transient, 0.0)
        assertEquals(parentB.harmonicity, child.harmonicity, 0.0)
        assertEquals(parentB.brightness, child.brightness, 0.0)
        assertEquals(parentB.dynamics, child.dynamics, 0.0)
        assertEquals(parentB.stereoWidth, child.stereoWidth, 0.0)
    }

    @Test
    fun breedRandom_takesMinConfidenceAndRecordsParents() {
        val breeder = SoundBreeder(Random(1))
        val child = breeder.breedRandom(parentA, parentB)

        assertEquals(minOf(parentA.confidence, parentB.confidence), child.confidence, 0.0)
        assertEquals(listOf(parentA.sourceId, parentB.sourceId), child.parents)
    }

    @Test
    fun breedRandom_rejectsBiasOutsideUnitRange() {
        val breeder = SoundBreeder()
        try {
            breeder.breedRandom(parentA, parentB, traitBiasTowardsB = 1.5)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun breedLitter_producesRequestedCountWithUniqueIds() {
        val breeder = SoundBreeder(Random(7))
        val litter = breeder.breedLitter(parentA, parentB, count = 5)

        assertEquals(5, litter.size)
        assertEquals(5, litter.map { it.sourceId }.toSet().size)
        litter.forEachIndexed { index, child ->
            assertTrue(child.sourceId.endsWith("#${index + 1}"))
        }
    }

    @Test
    fun breedLitter_rejectsNonPositiveCount() {
        val breeder = SoundBreeder()
        try {
            breeder.breedLitter(parentA, parentB, count = 0)
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun breedIntoArchive_addsRebornEntryWithOriginTakeId() {
        val breeder = SoundBreeder(Random(3))
        val archive = SoundArchive()

        val entry = breeder.breedIntoArchive(
            archive = archive,
            parentATakeId = parentA.sourceId,
            parentA = parentA,
            parentB = parentB,
            tags = setOf("experimental")
        )

        assertEquals(SoundArchive.State.REBORN, entry.state)
        assertEquals(parentA.sourceId, entry.originTakeId)
        assertEquals(setOf("experimental"), entry.tags)
        assertNotNull(entry.genome)

        val stored = archive.all().find { it.takeId == entry.takeId }
        assertNotNull(stored)
        assertEquals(entry, stored)
    }

    @Test
    fun closestMatch_returnsGenomeWithSmallestDistance() {
        val breeder = SoundBreeder()
        val target = SoundGenome("target", 0.5, 0.5, 0.5, 0.5)
        val near = SoundGenome("near", 0.51, 0.49, 0.5, 0.52)
        val far = SoundGenome("far", 0.0, 1.0, 0.0, 1.0)

        val result = breeder.closestMatch(target, listOf(far, near))

        assertEquals("near", result?.sourceId)
    }

    @Test
    fun closestMatch_returnsNullForEmptyCandidates() {
        val breeder = SoundBreeder()
        val target = SoundGenome("target", 0.5, 0.5, 0.5, 0.5)

        assertNull(breeder.closestMatch(target, emptyList()))
    }
}
