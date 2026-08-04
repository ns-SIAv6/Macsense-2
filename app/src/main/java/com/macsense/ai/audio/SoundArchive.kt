package com.macsense.ai.audio

import kotlinx.serialization.Serializable

/** Reversible lifecycle for discarded takes; deletion is a deliberate final action. */
class SoundArchive {
    enum class State { LIVING, DORMANT, REBORN }

    @Serializable
    data class Entry(
        val takeId: String,
        val state: State = State.LIVING,
        val tags: Set<String> = emptySet(),
        val genome: SoundGenome? = null,
        val originTakeId: String? = null
    )

    private val entries = linkedMapOf<String, Entry>()
    fun add(entry: Entry) { entries[entry.takeId] = entry }
    fun all(): List<Entry> = entries.values.toList()
    fun archive(takeId: String) { entries[takeId]?.let { entries[takeId] = it.copy(state = State.DORMANT) } }
    fun reborn(takeId: String, newTakeId: String): Entry? = entries[takeId]?.let { source -> Entry(newTakeId, State.REBORN, source.tags, source.genome, source.takeId).also { entries[newTakeId] = it } }
    fun findByTag(tag: String): List<Entry> = entries.values.filter { tag in it.tags }
}
