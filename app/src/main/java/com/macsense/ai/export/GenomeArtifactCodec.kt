package com.macsense.ai.export

import com.macsense.ai.audio.SoundArchive
import com.macsense.ai.audio.SoundGenome

/**
 * P5 flagship (issues #37, #61): cross-project genome export/import breeding.
 *
 * Wraps [GenomeShareableTrack] (the serialized Sound DNA artifact) with the archive-level
 * export/import rules that make the round-trip *breedable*:
 *  - export carries the genome plus a human-readable lineage summary;
 *  - import lands as a LIVING [SoundArchive.Entry] tagged "imported", with the artifact's
 *    ancestry (genome.parents, sourceId, confidence) fully preserved so local breeding keeps
 *    the cross-project lineage intact — the #61 acceptance boundary.
 *
 * Fails loudly on malformed artifacts instead of silently producing empty genomes.
 */
object GenomeArtifactCodec {

    const val IMPORTED_TAG = "imported"

    /** Builds the shareable Sound DNA artifact text for an archive entry that has a genome. */
    fun export(
        entry: SoundArchive.Entry,
        trackName: String,
        creatorName: String,
        exportedAt: Long,
        lineageSummary: String? = null,
    ): String {
        val genome = requireNotNull(entry.genome) {
            "Take ${entry.takeId} has no extracted genome; extract one before exporting Sound DNA"
        }
        val track = GenomeShareableTrack(
            genome = genome,
            trackName = trackName,
            creatorName = creatorName,
            exportedAt = exportedAt,
            tags = entry.tags.toList(),
            lineageSummary = lineageSummary,
        )
        return GenomeShareableTrack.toShareableJson(track)
    }

    /**
     * Parses a Sound DNA artifact and materializes it as an importable archive entry.
     * [newTakeId] is the local identity; the genome keeps its original sourceId + parents so
     * ancestry references survive the project boundary.
     */
    fun import(raw: String, newTakeId: String): SoundArchive.Entry {
        require(raw.isNotBlank()) { "Empty Sound DNA artifact" }
        val track = try {
            GenomeShareableTrack.fromShareableJson(raw)
        } catch (t: Throwable) {
            throw IllegalArgumentException("Not a valid MacSense Sound DNA artifact: ${t.message}", t)
        }
        return SoundArchive.Entry(
            takeId = newTakeId,
            state = SoundArchive.State.LIVING,
            tags = track.tags.toSet() + IMPORTED_TAG,
            genome = track.genome,
            originTakeId = null,
        )
    }
}
