package com.macsense.ai.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyllableBeatAlignerTest {

    @Test
    fun `syllable counting handles common shapes`() {
        assertEquals(1, SyllableBeatAligner.countSyllables("free"))
        assertEquals(2, SyllableBeatAligner.countSyllables("spilling"))
        assertEquals(2, SyllableBeatAligner.countSyllables("double"))
        assertEquals(3, SyllableBeatAligner.countSyllables("energy"))
        assertEquals(0, SyllableBeatAligner.countSyllables("!!!"))
        // Silent trailing e.
        assertEquals(1, SyllableBeatAligner.countSyllables("time"))
    }

    @Test
    fun `line counts sum words`() {
        assertEquals(4, SyllableBeatAligner.countLineSyllables("beat so hard now"))
    }

    @Test
    fun `alignment flags dense lines as off-grid`() {
        val sparse = "yeah"                                  // 1 syllable in 16 steps
        val dense = List(20) { "syllable" }.joinToString(" ") // 60 syllables in 16 steps
        val result = SyllableBeatAligner.align(listOf(sparse, dense))
        assertFalse(result[0].offGrid)
        assertTrue(result[1].offGrid)
        assertTrue(result[1].density > 1.0)
        assertEquals(16, result[0].availableSteps)
    }

    @Test
    fun `more bars per line means more available steps`() {
        val r = SyllableBeatAligner.align(listOf("some words here"), barsPerLine = 2)
        assertEquals(32, r[0].availableSteps)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero-step grid is rejected`() {
        SyllableBeatAligner.align(listOf("x"), stepsPerBar = 0)
    }
}
