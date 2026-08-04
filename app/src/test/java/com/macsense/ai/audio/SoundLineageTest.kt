package com.macsense.ai.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundLineageTest {

    private fun entry(
        takeId: String,
        parents: List<String> = emptyList(),
        originTakeId: String? = null,
        state: SoundArchive.State = SoundArchive.State.LIVING
    ) = SoundArchive.Entry(
        takeId = takeId,
        state = state,
        genome = SoundGenome(sourceId = takeId, transient = 0.5, harmonicity = 0.5, brightness = 0.5, dynamics = 0.5, parents = parents),
        originTakeId = originTakeId
    )

    @Test
    fun breedingParents_returnsBothParentsFromGenome() {
        val parentA = entry("a")
        val parentB = entry("b")
        val child = entry("a×b", parents = listOf("a", "b"))
        val lineage = SoundLineage(listOf(parentA, parentB, child))

        val parents = lineage.breedingParents("a×b")
        assertEquals(setOf("a", "b"), parents.map { it.takeId }.toSet())
    }

    @Test
    fun breedingParents_returnsEmptyForUnknownTake() {
        val lineage = SoundLineage(emptyList())
        assertTrue(lineage.breedingParents("ghost").isEmpty())
    }

    @Test
    fun resurrectionParent_followsOriginTakeId() {
        val dormant = entry("dormant1")
        val reborn = entry("reborn1", originTakeId = "dormant1", state = SoundArchive.State.REBORN)
        val lineage = SoundLineage(listOf(dormant, reborn))

        assertEquals("dormant1", lineage.resurrectionParent("reborn1")?.takeId)
    }

    @Test
    fun resurrectionParent_isNullWhenNoOrigin() {
        val lineage = SoundLineage(listOf(entry("solo")))
        assertNull(lineage.resurrectionParent("solo"))
    }

    @Test
    fun ancestors_combinesBreedingAndResurrectionTransitively() {
        val grandparentA = entry("gpa")
        val grandparentB = entry("gpb")
        val parent = entry("parent", parents = listOf("gpa", "gpb"))
        val dormantAncestor = entry("dormantAncestor")
        val child = entry("child", parents = listOf("parent"), originTakeId = "dormantAncestor")
        val lineage = SoundLineage(listOf(grandparentA, grandparentB, parent, dormantAncestor, child))

        val ancestors = lineage.ancestors("child").map { it.takeId }.toSet()
        assertEquals(setOf("parent", "dormantAncestor", "gpa", "gpb"), ancestors)
    }

    @Test
    fun ancestors_isCycleSafe() {
        val a = entry("a", originTakeId = "b")
        val b = entry("b", originTakeId = "a")
        val lineage = SoundLineage(listOf(a, b))

        val ancestors = lineage.ancestors("a")
        assertEquals(1, ancestors.size)
        assertEquals("b", ancestors.first().takeId)
    }

    @Test
    fun descendants_findsChildrenTransitively() {
        val root = entry("root")
        val child1 = entry("child1", parents = listOf("root"))
        val grandchild = entry("grandchild", originTakeId = "child1")
        val lineage = SoundLineage(listOf(root, child1, grandchild))

        val descendants = lineage.descendants("root").map { it.takeId }.toSet()
        assertEquals(setOf("child1", "grandchild"), descendants)
    }

    @Test
    fun descendants_isCycleSafe() {
        val a = entry("a", originTakeId = "b")
        val b = entry("b", originTakeId = "a")
        val lineage = SoundLineage(listOf(a, b))

        val descendants = lineage.descendants("a")
        assertEquals(1, descendants.size)
        assertEquals("b", descendants.first().takeId)
    }

    @Test
    fun resurrectionChain_ordersOldestFirstIncludingSelf() {
        val grandOrigin = entry("grandOrigin")
        val origin = entry("origin", originTakeId = "grandOrigin", state = SoundArchive.State.REBORN)
        val self = entry("self", originTakeId = "origin", state = SoundArchive.State.REBORN)
        val lineage = SoundLineage(listOf(grandOrigin, origin, self))

        val chain = lineage.resurrectionChain("self").map { it.takeId }
        assertEquals(listOf("grandOrigin", "origin", "self"), chain)
    }

    @Test
    fun resurrectionChain_returnsEmptyForUnknownTake() {
        val lineage = SoundLineage(emptyList())
        assertTrue(lineage.resurrectionChain("ghost").isEmpty())
    }

    @Test
    fun generationDepth_countsResurrectionHops() {
        val grandOrigin = entry("grandOrigin")
        val origin = entry("origin", originTakeId = "grandOrigin", state = SoundArchive.State.REBORN)
        val self = entry("self", originTakeId = "origin", state = SoundArchive.State.REBORN)
        val lineage = SoundLineage(listOf(grandOrigin, origin, self))

        assertEquals(2, lineage.generationDepth("self"))
        assertEquals(0, lineage.generationDepth("grandOrigin"))
    }

    @Test
    fun rootTakes_returnsOnlyEntriesWithNoAncestry() {
        val root1 = entry("root1")
        val root2 = entry("root2")
        val bred = entry("bred", parents = listOf("root1", "root2"))
        val reborn = entry("reborn", originTakeId = "root1")
        val lineage = SoundLineage(listOf(root1, root2, bred, reborn))

        val roots = lineage.rootTakes().map { it.takeId }.toSet()
        assertEquals(setOf("root1", "root2"), roots)
    }
}
