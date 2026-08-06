package com.macsense.ai.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricEditEngineTest {

    private val sample = "Yeah, double cup spilling on the MPC\nBeat so hard, MACSENSE setting me free"

    @Test
    fun propose_neverMutatesOriginalSelection() {
        val diff = LyricEditEngine.propose(sample, LyricEditAction.REWRITE, seed = 1)
        assertEquals(sample, diff.original)
    }

    @Test
    fun reject_leavesFullTextUnchanged() {
        val diff = LyricEditEngine.propose(sample, LyricEditAction.MAKE_MORE_AGGRESSIVE)
        val fullText = "intro line\n$sample\noutro line"

        val result = diff.reject(fullText)

        assertEquals(fullText, result)
    }

    @Test
    fun accept_replacesOnlyTheOriginalSpanWithinFullText() {
        val diff = LyricEditEngine.propose(sample, LyricEditAction.MAKE_MORE_AGGRESSIVE)
        val fullText = "intro line\n$sample\noutro line"

        val result = diff.accept(fullText)

        assertTrue(result.contains("intro line"))
        assertTrue(result.contains("outro line"))
        assertFalse(result.contains(sample))
        assertTrue(result.contains(diff.proposed))
    }

    @Test
    fun accept_isNoopWhenOriginalSpanNotFoundInFullText() {
        val diff = LyricEditEngine.propose(sample, LyricEditAction.REWRITE, seed = 2)
        val unrelatedText = "totally different lyrics here"

        val result = diff.accept(unrelatedText)

        assertEquals(unrelatedText, result)
    }

    @Test
    fun makeMoreAggressive_swapsKnownSoftWordsForHarderSynonyms() {
        val diff = LyricEditEngine.propose("I like to walk and say nice things", LyricEditAction.MAKE_MORE_AGGRESSIVE)

        assertTrue(diff.proposed.contains("crave"))
        assertTrue(diff.proposed.contains("storm"))
        assertTrue(diff.proposed.contains("scream"))
        assertTrue(diff.proposed.contains("vicious"))
    }

    @Test
    fun improveRhyme_alignsEarlierLineEndingsTowardFinalLineSuffix() {
        val input = "Chasing the light\nLiving for tonight\nDreaming of the sky"

        val diff = LyricEditEngine.propose(input, LyricEditAction.IMPROVE_RHYME)

        val lines = diff.proposed.split("\n")
        assertEquals("Dreaming of the sky", lines.last())
        assertTrue(lines[0].endsWith("-ky") || lines[0].endsWith("light"))
    }

    @Test
    fun improveRhyme_isNoopForSingleLineInput() {
        val diff = LyricEditEngine.propose("just one line here", LyricEditAction.IMPROVE_RHYME)

        assertTrue(diff.isNoop)
    }

    @Test
    fun betterCadence_normalizesLineWordCountsTowardAverage() {
        val input = "one two three four five six seven eight\nshort line"

        val diff = LyricEditEngine.propose(input, LyricEditAction.BETTER_CADENCE)

        val lines = diff.proposed.split("\n")
        val counts = lines.map { it.trim().split(Regex("\\s+")).size }
        assertTrue(counts.max() - counts.min() <= 1)
    }

    @Test
    fun changeFlow_prependsDeliveryConnectorToFirstLineOnly() {
        val input = "double cup spilling on the MPC\nbeat so hard"

        val diff = LyricEditEngine.propose(input, LyricEditAction.CHANGE_FLOW, seed = 0)

        val lines = diff.proposed.split("\n")
        assertTrue(lines[0].startsWith("Yeah,") || lines[0].startsWith("Listen,") || lines[0].startsWith("Check it,") || lines[0].startsWith("Straight up,") || lines[0].startsWith("No cap,"))
        assertEquals("beat so hard", lines[1])
    }

    @Test
    fun propose_generatesDeterministicIdPerActionAndSeed() {
        val diffA = LyricEditEngine.propose(sample, LyricEditAction.REWRITE, seed = 5)
        val diffB = LyricEditEngine.propose(sample, LyricEditAction.REWRITE, seed = 5)

        assertEquals(diffA.id, diffB.id)
    }

    @Test
    fun propose_producesDistinctIdsForDifferentActions() {
        val rewriteDiff = LyricEditEngine.propose(sample, LyricEditAction.REWRITE, seed = 0)
        val aggressiveDiff = LyricEditEngine.propose(sample, LyricEditAction.MAKE_MORE_AGGRESSIVE, seed = 0)

        assertFalse(rewriteDiff.id == aggressiveDiff.id)
    }
}
