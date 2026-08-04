package com.macsense.ai.audio

import kotlin.random.Random

/**
 * Pure-Kotlin orchestration layer on top of [SoundGenome.breed]. Where [SoundGenome.breed]
 * is a single deterministic trait-swap operation, [SoundBreeder] adds the higher-level
 * "studio" behaviors needed by a breeding UI: randomized trait inheritance, batch/generation
 * breeding, sibling variation, and wiring bred genomes straight into a [SoundArchive] as new
 * REBORN entries so lineage is preserved automatically.
 *
 * This class has no Android or DB dependencies so it is fully unit-testable in the `test`
 * source set.
 */
class SoundBreeder(private val random: Random = Random.Default) {

    /**
     * Breeds [parentA] and [parentB] by randomly choosing, per trait, which parent's value the
     * offspring inherits. Each trait is inherited from [parentB] independently with probability
     * [traitBiasTowardsB] (default 0.5, i.e. an unbiased coin flip per trait).
     */
    fun breedRandom(
        parentA: SoundGenome,
        parentB: SoundGenome,
        traitBiasTowardsB: Double = 0.5
    ): SoundGenome {
        require(traitBiasTowardsB in 0.0..1.0) { "traitBiasTowardsB must be in 0.0..1.0" }
        val traitsFromB = SoundGenome.Trait.values()
            .filter { random.nextDouble() < traitBiasTowardsB }
            .toSet()
        return parentA.breed(parentB, traitsFromB)
    }

    /**
     * Produces [count] sibling genomes from the same two parents, each with an independently
     * randomized trait inheritance pattern, so a breeding UI can offer several candidate
     * offspring to choose from in one pass. Sibling `sourceId`s are disambiguated with a
     * numeric suffix since [SoundGenome.breed] otherwise gives all siblings the same id.
     */
    fun breedLitter(
        parentA: SoundGenome,
        parentB: SoundGenome,
        count: Int,
        traitBiasTowardsB: Double = 0.5
    ): List<SoundGenome> {
        require(count > 0) { "count must be > 0" }
        return (1..count).map { index ->
            val child = breedRandom(parentA, parentB, traitBiasTowardsB)
            child.copy(sourceId = "${child.sourceId}#$index")
        }
    }

    /**
     * Breeds [parentA] and [parentB] and immediately archives the offspring into [archive] as
     * a new REBORN entry whose [SoundArchive.Entry.originTakeId] points at [parentA]'s take id,
     * so the lineage graph can trace the child back to at least one parent. Returns the new
     * archive entry. [tags] let callers seed the child with searchable metadata (e.g. genre or
     * mood tags) at creation time.
     */
    fun breedIntoArchive(
        archive: SoundArchive,
        parentATakeId: String,
        parentA: SoundGenome,
        parentB: SoundGenome,
        traitBiasTowardsB: Double = 0.5,
        tags: Set<String> = emptySet()
    ): SoundArchive.Entry {
        val childGenome = breedRandom(parentA, parentB, traitBiasTowardsB)
        val entry = SoundArchive.Entry(
            takeId = childGenome.sourceId,
            state = SoundArchive.State.REBORN,
            tags = tags,
            genome = childGenome,
            originTakeId = parentATakeId
        )
        archive.add(entry)
        return entry
    }

    /**
     * Finds the closest match to [target] among [candidates] using [SoundGenome.distanceFrom],
     * useful for a "breed towards this reference sound" workflow. Returns null if [candidates]
     * is empty.
     */
    fun closestMatch(target: SoundGenome, candidates: List<SoundGenome>): SoundGenome? =
        candidates.minByOrNull { it.distanceFrom(target) }
}
