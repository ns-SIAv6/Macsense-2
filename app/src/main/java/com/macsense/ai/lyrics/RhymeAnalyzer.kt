package com.macsense.ai.lyrics

/**
 * P7 (issue #42): rhyme intelligence.
 *
 * Detects end rhymes, near rhymes, and internal rhymes across lyric lines using a
 * phonetic-ish "rhyme key": the trailing vowel-cluster + consonant tail of a word,
 * normalized for common English spelling variance. Deliberately dictionary-free so it
 * works offline for slang/ad-libs; Ari can layer smarter suggestions on top.
 */
object RhymeAnalyzer {

    data class WordRef(val word: String, val lineIndex: Int, val wordIndex: Int)

    data class RhymeGroup(val key: String, val members: List<WordRef>, val kind: Kind) {
        enum class Kind { END, INTERNAL }
    }

    private val NON_WORD = Regex("[^a-z0-9']")
    private val VOWELS = "aeiouy".toSet()

    fun normalize(word: String): String = word.lowercase().replace(NON_WORD, "")

    /**
     * The rhyme key: substring from the last stressed-ish vowel cluster to the end,
     * with light normalization (drop trailing silent-e, collapse doubled consonants).
     */
    fun rhymeKey(rawWord: String): String? {
        var w = normalize(rawWord)
        if (w.length < 2) return null
        if (w.endsWith("e") && w.length > 2 && w[w.length - 2] !in VOWELS) w = w.dropLast(1)
        // Trailing w after a vowel is part of the vowel sound ("flow" ~ "go").
        if (w.endsWith("w") && w.length > 1 && w[w.length - 2] in VOWELS) w = w.dropLast(1)
        // find start of last vowel cluster
        var i = w.length - 1
        while (i >= 0 && w[i] !in VOWELS) i--
        if (i < 0) return null
        while (i > 0 && w[i - 1] in VOWELS) i--
        var key = w.substring(i)
        key = Regex("(.)\\1+").replace(key, "$1") // collapse doubles
        key = key.replace('y', 'i') // y and i share the vowel sound in rhyme position
        return key
    }

    /** Perfect rhyme: identical keys on different words. */
    fun isRhyme(a: String, b: String): Boolean {
        val ka = rhymeKey(a) ?: return false
        val kb = rhymeKey(b) ?: return false
        return ka == kb && normalize(a) != normalize(b)
    }

    /** Near rhyme: same vowel nucleus, differing consonant tail (assonance). */
    fun isNearRhyme(a: String, b: String): Boolean {
        if (isRhyme(a, b)) return false
        val ka = rhymeKey(a) ?: return false
        val kb = rhymeKey(b) ?: return false
        val va = ka.takeWhile { it in VOWELS }
        val vb = kb.takeWhile { it in VOWELS }
        return va.isNotEmpty() && va == vb && normalize(a) != normalize(b)
    }

    private fun words(line: String): List<String> =
        line.split(Regex("\\s+")).map { it.trim() }.filter { normalize(it).isNotEmpty() }

    /** Groups line-final words that rhyme with each other (2+ members). */
    fun endRhymeGroups(lines: List<String>): List<RhymeGroup> {
        val finals = lines.mapIndexedNotNull { li, line ->
            words(line).let { ws ->
                ws.lastOrNull()?.let { WordRef(it, li, ws.size - 1) }
            }
        }
        return finals
            .mapNotNull { ref -> rhymeKey(ref.word)?.let { it to ref } }
            .groupBy({ it.first }, { it.second })
            .filter { (_, refs) -> refs.map { normalize(it.word) }.distinct().size >= 2 }
            .map { (key, refs) -> RhymeGroup(key, refs, RhymeGroup.Kind.END) }
    }

    /** Rhyming word pairs inside a single line (internal rhymes). */
    fun internalRhymes(line: String, lineIndex: Int = 0): List<RhymeGroup> {
        val ws = words(line)
        val byKey = ws.mapIndexedNotNull { wi, w ->
            rhymeKey(w)?.let { it to WordRef(w, lineIndex, wi) }
        }.groupBy({ it.first }, { it.second })
        return byKey
            .filter { (_, refs) -> refs.map { normalize(it.word) }.distinct().size >= 2 }
            .map { (key, refs) -> RhymeGroup(key, refs, RhymeGroup.Kind.INTERNAL) }
    }

    /** Ranks candidate replacement words by rhyme quality against a target. */
    fun suggestAlternatives(target: String, candidates: List<String>): List<String> =
        candidates
            .filter { normalize(it) != normalize(target) }
            .map { it to score(target, it) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }

    private fun score(target: String, candidate: String): Int = when {
        isRhyme(target, candidate) -> 2
        isNearRhyme(target, candidate) -> 1
        else -> 0
    }
}
