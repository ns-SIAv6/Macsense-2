package com.macsense.ai.export

import com.macsense.ai.data.local.ProjectEntity
import com.macsense.ai.audio.SoundArchive
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Multi-version export factory for full project exports.
 *
 * Produces structured export bundles containing:
 * - Project metadata (name, BPM, creation time)
 * - All archive entries with their state and tags
 * - Genome artifacts for each entry (if available)
 * - Version history summary
 *
 * Supported output formats:
 * - JSON  (v1): flat bundle, backward-compatible
 * - JSON  (v2): nested with per-entry genome data
 * - Shareable ZIP manifest (future: produce URI for Android share sheet)
 */
object ProjectExportFactory {

    @Serializable
    data class ExportBundle(
        val version: String,
        val projectId: String,
        val projectName: String,
        val bpm: Double,
        val exportedAtMs: Long = System.currentTimeMillis(),
        val archiveEntries: List<ArchiveEntryExport>,
        val genomeArtifacts: List<GenomeArtifact>
    )

    @Serializable
    data class ArchiveEntryExport(
        val takeId: String,
        val state: String,
        val tags: List<String>,
        val originTakeId: String?
    )

    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

    /**
     * Builds a v1 export: project metadata + archive entry list.
     * No genome data. Smallest output, maximally compatible.
     */
    fun exportV1(project: ProjectEntity, entries: List<SoundArchive.Entry>): String {
        val bundle = ExportBundle(
            version = "1.0",
            projectId = project.id,
            projectName = project.name,
            bpm = project.bpm,
            archiveEntries = entries.map { it.toExport() },
            genomeArtifacts = emptyList()
        )
        return json.encodeToString(ExportBundle.serializer(), bundle)
    }

    /**
     * Builds a v2 export: includes genome artifacts for each entry.
     * Larger but genome-shareable: the recipient can import the whole bundle.
     */
    fun exportV2(
        project: ProjectEntity,
        entries: List<SoundArchive.Entry>,
        artifacts: Map<String, GenomeArtifact>
    ): String {
        val bundle = ExportBundle(
            version = "2.0",
            projectId = project.id,
            projectName = project.name,
            bpm = project.bpm,
            archiveEntries = entries.map { it.toExport() },
            genomeArtifacts = entries.mapNotNull { artifacts[it.takeId] }
        )
        return json.encodeToString(ExportBundle.serializer(), bundle)
    }

    /** Parses an export bundle back for import. Throws on malformed JSON. */
    fun decode(raw: String): ExportBundle =
        json.decodeFromString(ExportBundle.serializer(), raw)

    private fun SoundArchive.Entry.toExport() = ArchiveEntryExport(
        takeId = takeId,
        state = state.name,
        tags = tags.toList(),
        originTakeId = originTakeId
    )
}
