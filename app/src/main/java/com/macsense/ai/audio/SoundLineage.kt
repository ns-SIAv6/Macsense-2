package com.macsense.ai.audio

/**
 * Pure-Kotlin ancestry graph over a snapshot of [SoundArchive.Entry] records. Where
 * [SoundArchive] itself only stores entries, and [SoundBreeder] only creates offspring,
 * [SoundLineage] answers questions about *how takes relate to each other* over time: which
 * takes bred a given take, which takes it resurrected from, and the full family tree either
 * direction. This backs the "lineage graph" referenced throughout the Phase 5 genetic-sound
 * design without requiring a live [SoundArchive] instance — it works off any snapshot of
 * entries, e.g. one loaded from [com.macsense.ai.data.repository.MacSenseRepository.getArchiveEntries].
 *
 * Two distinct kinds of ancestry are tracked and kept separate so callers can distinguish
 * "this sound was bred from those two sounds" from "this sound was resurrected from that
 * dormant sound":
 * - Breeding ancestry comes from [SoundGenome.parents] on the entry's genome (populated by
 *   [SoundGenome.breed] / [SoundBreeder]).
 * - Resurrection ancestry comes from [SoundArchive.Entry.originTakeId] (populated by
 *   [SoundArchive.reborn] / [SoundBreeder.breedIntoArchive]).
 */
class SoundLineage(entries: List<SoundArchive.Entry>) {
    private val byTakeId: Map<String, SoundArchive.Entry> = entries.associateBy { it.takeId }

    /** Direct breeding parents of [takeId], from its genome's [SoundGenome.parents], if known in this snapshot. */
    fun breedingParents(takeId: String): List<SoundArchive.Entry> {
        val genome = byTakeId[takeId]?.genome ?: return emptyList()
        return genome.parents.mapNotNull { byTakeId[it] }
    }

    /** The take [takeId] was directly resurrected from, if any, via [SoundArchive.Entry.originTakeId]. */
    fun resurrectionParent(takeId: String): SoundArchive.Entry? {
        val originId = byTakeId[takeId]?.originTakeId ?: return null
        return byTakeId[originId]
    }

    /**
     * All ancestors of [takeId] found by walking both breeding parents and resurrection origin
     * transitively, deepest-last. Cycle-safe: a take already visited is never revisited, so a
     * malformed/cyclic snapshot terminates instead of looping forever.
     */
    fun ancestors(takeId: String): List<SoundArchive.Entry> {
        val visited = linkedSetOf<String>()
        val result = mutableListOf<SoundArchive.Entry>()
        val queue = ArrayDeque<String>()
        queue.add(takeId)
        visited.add(takeId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val parents = breedingParents(current) + listOfNotNull(resurrectionParent(current))
            for (parent in parents) {
                if (visited.add(parent.takeId)) {
                    result.add(parent)
                    queue.add(parent.takeId)
                }
            }
        }
        return result
    }

    /**
     * All descendants of [takeId]: every entry in the snapshot whose breeding parents or
     * resurrection origin transitively include [takeId]. Cycle-safe like [ancestors].
     */
    fun descendants(takeId: String): List<SoundArchive.Entry> {
        val visited = linkedSetOf<String>()
        val result = mutableListOf<SoundArchive.Entry>()
        val queue = ArrayDeque<String>()
        queue.add(takeId)
        visited.add(takeId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val children = byTakeId.values.filter { entry ->
                current in (entry.genome?.parents ?: emptyList()) || entry.originTakeId == current
            }
            for (child in children) {
                if (visited.add(child.takeId)) {
                    result.add(child)
                    queue.add(child.takeId)
                }
            }
        }
        return result
    }

    /**
     * The chain of REBORN resurrections leading to [takeId], oldest first, followed by
     * [takeId] itself. E.g. for a take resurrected from a dormant take that was itself
     * resurrected once before, this returns `[grandOrigin, origin, takeId's entry]`.
     * Returns an empty list if [takeId] is not present in this snapshot.
     */
    fun resurrectionChain(takeId: String): List<SoundArchive.Entry> {
        val self = byTakeId[takeId] ?: return emptyList()
        val chain = mutableListOf(self)
        val visited = mutableSetOf(takeId)
        var current = self
        while (true) {
            val parent = current.originTakeId?.let { byTakeId[it] } ?: break
            if (!visited.add(parent.takeId)) break // guard against cyclic origin references
            chain.add(0, parent)
            current = parent
        }
        return chain
    }

    /** Number of resurrection hops between [takeId] and its oldest known resurrection ancestor. */
    fun generationDepth(takeId: String): Int = (resurrectionChain(takeId).size - 1).coerceAtLeast(0)

    /** All entries in this snapshot with no known breeding parents and no resurrection origin — the roots of the family tree. */
    fun rootTakes(): List<SoundArchive.Entry> = byTakeId.values.filter {
        it.genome?.parents.isNullOrEmpty() && it.originTakeId == null
    }
}
