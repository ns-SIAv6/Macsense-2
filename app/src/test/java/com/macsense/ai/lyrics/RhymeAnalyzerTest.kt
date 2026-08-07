package com.macsense.ai.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RhymeAnalyzerTest {

    @Test
    fun `perfect end rhymes are detected`() {
        assertTrue(RhymeAnalyzer.isRhyme("free", "me"))
        assertTrue(RhymeAnalyzer.isRhyme("night", "light"))
        assertTrue(RhymeAnalyzer.isRhyme("flow", "go"))
        assertFalse(RhymeAnalyzer.isRhyme("night", "day"))
        // A word does not rhyme with itself.
        assertFalse(RhymeAnalyzer.isRhyme("night", "night"))
        assertFalse(RhymeAnalyzer.isRhyme("Night", "night!"))
    }

    @Test
    fun `near rhymes share the vowel nucleus`() {
        assertTrue(RhymeAnalyzer.isNearRhyme("cat", "map"))
        assertTrue(RhymeAnalyzer.isNearRhyme("time", "life") || RhymeAnalyzer.isRhyme("time", "life"))
        assertFalse(RhymeAnalyzer.isNearRhyme("night", "light")) // that's a full rhyme
        assertFalse(RhymeAnalyzer.isNearRhyme("cat", "dog"))
    }

    @Test
    fun `end rhyme groups span lines`() {
        val lines = listOf(
            "Blowing smoke, man I feel so free",
            "Beat so hard, MACSENSE carrying me",
            "Midnight grind, we just having fun",
            "Sessions running till I see the sun",
        )
        val groups = RhymeAnalyzer.endRhymeGroups(lines)
        assertEquals(2, groups.size)
        val lineSets = groups.map { g -> g.members.map { it.lineIndex }.toSet() }
        assertTrue(setOf(0, 1) in lineSets)
        assertTrue(setOf(2, 3) in lineSets)
    }

    @Test
    fun `internal rhymes inside one line`() {
        val groups = RhymeAnalyzer.internalRhymes("I grind all the time with a rhyme in my mind")
        assertTrue(groups.isNotEmpty())
        val members = groups.flatMap { it.members }.map { RhymeAnalyzer.normalize(it.word) }.toSet()
        assertTrue("time" in members && "rhyme" in members)
    }

    @Test
    fun `alternatives are ranked by rhyme quality`() {
        val ranked = RhymeAnalyzer.suggestAlternatives(
            target = "night",
            candidates = listOf("dog", "light", "time", "sight", "map"),
        )
        assertEquals(listOf("light", "sight", "time"), ranked)
    }

    @Test
    fun `degenerate input never crashes`() {
        assertTrue(RhymeAnalyzer.endRhymeGroups(emptyList()).isEmpty())
        assertTrue(RhymeAnalyzer.internalRhymes("").isEmpty())
        assertFalse(RhymeAnalyzer.isRhyme("", "x"))
        assertEquals(null, RhymeAnalyzer.rhymeKey("!!!"))
    }
}
