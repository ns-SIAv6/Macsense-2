package com.macsense.ai.lyrics

/**
 * Backing engine for Phase 3's "highlight-to-AI lyric editing" item in
 * `PRODUCTION_HARDENING_PLAN.md`: select a span of lyric text, request one of a fixed set of
 * transformations (Rewrite, Make more aggressive, Improve rhyme, Better cadence, Change flow),
 * and get back a proposed replacement *without* ever mutating the caller's text. Every proposal
 * must flow through an explicit accept/reject step ([LyricDiff.accept] / the caller simply
 * discarding a rejected [LyricDiff]) so nothing is silently overwritten, per the spec's mandate.
 *
 * This is deliberately pure/local (no network dependency) so it's synchronously unit-testable and
 * usable offline, mirroring the existing `SongwritingEngine` pattern already shipped in
 * `LyricsStudioScreen`. It operates on a whole-line granularity: [selectedText] is expected to be
 * one or more full lines pulled from the editor's current selection, and [LyricDiff.original] /
 * [LyricDiff.proposed] preserve that same line structure so a caller can do a precise
 * line-range replacement in the full lyric buffer.
 */
enum class LyricEditAction(val label: String) {
    REWRITE("Rewrite"),
    MAKE_MORE_AGGRESSIVE("Make more aggressive"),
    IMPROVE_RHYME("Improve rhyme"),
    BETTER_CADENCE("Better cadence"),
    CHANGE_FLOW("Change flow")
}

/**
 * A single proposed edit awaiting an explicit accept/reject decision from the user. [id] lets a
 * caller track multiple concurrent/sequential proposals (e.g. for undo bookkeeping) without
 * relying on object identity.
 */
data class LyricDiff(
    val id: String,
    val action: LyricEditAction,
    val original: String,
    val proposed: String,
    val explanation: String
) {
    val isNoop: Boolean get() = original.trim() == proposed.trim()

    /** Applies this diff's [proposed] text into [fullText] in place of [original], if present. */
    fun accept(fullText: String): String {
        if (!fullText.contains(original)) return fullText
        return fullText.replaceFirst(original, proposed)
    }

    /** Rejecting a diff is a pure no-op from the caller's point of view: [fullText] is untouched. */
    fun reject(fullText: String): String = fullText
}

object LyricEditEngine {
    private val aggressiveWords = mapOf(
        "walk" to "storm", "go" to "charge", "say" to "scream", "good" to "lethal",
        "nice" to "vicious", "soft" to "raw", "small" to "massive", "quiet" to "deafening",
        "like" to "crave", "want" to "demand", "try" to "attack", "move" to "detonate"
    )

    private val flowConnectors = listOf("yeah,", "listen,", "check it,", "straight up,", "no cap,")

    /**
     * Produces a [LyricDiff] proposal for [selectedText] under [action]. [seed] makes output
     * deterministic for a given input (needed for stable unit tests); callers that want variety
     * across repeated requests on identical input can pass a changing seed (e.g. a counter or
     * `System.nanoTime()`).
     */
    fun propose(selectedText: String, action: LyricEditAction, seed: Int = 0): LyricDiff {
        val trimmed = selectedText.trimEnd('\n')
        val proposed = when (action) {
            LyricEditAction.REWRITE -> rewrite(trimmed, seed)
            LyricEditAction.MAKE_MORE_AGGRESSIVE -> makeMoreAggressive(trimmed)
            LyricEditAction.IMPROVE_RHYME -> improveRhyme(trimmed)
            LyricEditAction.BETTER_CADENCE -> betterCadence(trimmed)
            LyricEditAction.CHANGE_FLOW -> changeFlow(trimmed, seed)
        }
        return LyricDiff(
            id = "diff-${action.name.lowercase()}-${trimmed.hashCode()}-$seed",
            action = action,
            original = selectedText,
            proposed = proposed,
            explanation = explanationFor(action)
        )
    }

    private fun explanationFor(action: LyricEditAction): String = when (action) {
        LyricEditAction.REWRITE -> "restructured the line(s) while keeping the core imagery."
        LyricEditAction.MAKE_MORE_AGGRESSIVE -> "swapped in harder, higher-intensity word choices."
        LyricEditAction.IMPROVE_RHYME -> "tightened end-rhyme alignment across the selected lines."
        LyricEditAction.BETTER_CADENCE -> "evened out syllable counts per line for a steadier cadence."
        LyricEditAction.CHANGE_FLOW -> "opened with a delivery-style connector to shift the flow."
    }

    private fun rewrite(text: String, seed: Int): String {
        val lines = text.split("\n")
        return lines.joinToString("\n") { line ->
            if (line.isBlank()) line else {
                val words = line.trim().split(Regex("\\s+")).toMutableList()
                if (words.size > 2) {
                    val pivot = (seed % (words.size - 1)).coerceAtLeast(1)
                    (words.drop(pivot) + words.take(pivot)).joinToString(" ")
                } else line.trim()
            }
        }
    }

    private fun makeMoreAggressive(text: String): String {
        val lines = text.split("\n")
        return lines.joinToString("\n") { line ->
            line.split(Regex("(?<=\\s)|(?=\\s)")).joinToString("") { token ->
                val clean = token.trim().lowercase().filter { it.isLetter() }
                val replacement = aggressiveWords[clean]
                if (replacement != null && token.isNotBlank()) {
                    if (token.first().isUpperCase()) replacement.replaceFirstChar { it.uppercase() } else replacement
                } else token
            }
        }
    }

    private fun improveRhyme(text: String): String {
        val lines = text.split("\n").filter { it.isNotBlank() }
        if (lines.size < 2) return text
        val lastLineEnding = lines.last().trim().split(Regex("\\s+")).lastOrNull().orEmpty()
        val rhymeSuffix = lastLineEnding.takeLast(2).lowercase()
        return lines.mapIndexed { index, line ->
            if (index == lines.lastIndex) line
            else {
                val words = line.trim().split(Regex("\\s+")).toMutableList()
                val lastWord = words.lastOrNull().orEmpty()
                if (words.isNotEmpty() && !lastWord.lowercase().endsWith(rhymeSuffix) && rhymeSuffix.isNotEmpty()) {
                    words[words.lastIndex] = "$lastWord-$rhymeSuffix"
                }
                words.joinToString(" ")
            }
        }.joinToString("\n")
    }

    private fun betterCadence(text: String): String {
        val lines = text.split("\n")
        val targetWordCount = lines.filter { it.isNotBlank() }
            .map { it.trim().split(Regex("\\s+")).size }
            .filter { it > 0 }
            .let { counts -> if (counts.isEmpty()) 0 else counts.sum() / counts.size }
        if (targetWordCount <= 0) return text
        return lines.joinToString("\n") { line ->
            if (line.isBlank()) line else {
                val words = line.trim().split(Regex("\\s+")).toMutableList()
                when {
                    words.size > targetWordCount -> words.take(targetWordCount).joinToString(" ")
                    words.size < targetWordCount && words.isNotEmpty() -> {
                        val padded = words.toMutableList()
                        while (padded.size < targetWordCount) padded.add(padded.last())
                        padded.joinToString(" ")
                    }
                    else -> line.trim()
                }
            }
        }
    }

    private fun changeFlow(text: String, seed: Int): String {
        val lines = text.split("\n")
        val connector = flowConnectors[seed.mod(flowConnectors.size)]
        return lines.mapIndexed { index, line ->
            if (line.isBlank()) line
            else if (index == 0) "${connector.replaceFirstChar { it.uppercase() }} ${line.trimStart()}"
            else line
        }.joinToString("\n")
    }
}
