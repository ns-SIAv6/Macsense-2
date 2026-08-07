package com.macsense.ai.lyrics

/**
 * P7 (issue #42): syllable-to-beat alignment.
 *
 * Counts syllables per lyric line and maps them onto the DAW beat grid (16 steps/bar by
 * default, matching the instrument grid), flagging lines whose syllable load can't sit on
 * the grid cleanly — the "off-grid phrase" visual warning.
 */
object SyllableBeatAligner {

    data class LineAlignment(
        val lineIndex: Int,
        val text: String,
        val syllables: Int,
        val availableSteps: Int,
        /** Syllables per step; > 1.0 means the phrase is denser than the grid. */
        val density: Double,
        val offGrid: Boolean,
    )

    private val VOWEL_GROUP = Regex("[aeiouy]+")
    private val NON_WORD = Regex("[^a-z']")

    /** Heuristic English syllable count; never returns less than 1 for a real word. */
    fun countSyllables(word: String): Int {
        val w = word.lowercase().replace(NON_WORD, "")
        if (w.isEmpty()) return 0
        var count = VOWEL_GROUP.findAll(w).count()
        if (w.endsWith("e") && !w.endsWith("le") && count > 1) count--
        return count.coerceAtLeast(1)
    }

    fun countLineSyllables(line: String): Int =
        line.split(Regex("\\s+")).sumOf { countSyllables(it) }

    /**
     * Aligns lyric lines against [barsPerLine] bars of [stepsPerBar] steps each.
     * A line is off-grid when its density exceeds [maxSyllablesPerStep].
     */
    fun align(
        lines: List<String>,
        barsPerLine: Int = 1,
        stepsPerBar: Int = 16,
        maxSyllablesPerStep: Double = 1.0,
    ): List<LineAlignment> {
        require(barsPerLine > 0 && stepsPerBar > 0) { "Grid must have positive dimensions" }
        val steps = barsPerLine * stepsPerBar
        return lines.mapIndexed { i, line ->
            val syl = countLineSyllables(line)
            val density = syl.toDouble() / steps
            LineAlignment(
                lineIndex = i,
                text = line,
                syllables = syl,
                availableSteps = steps,
                density = density,
                offGrid = density > maxSyllablesPerStep,
            )
        }
    }
}
