package com.macsense.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.ForeignKey

@Entity(
    tableName = "projects",
    indices = [Index(value = ["name"])]
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "bpm", defaultValue = "120.0") val bpm: Double
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val orderIndex: Int,
    /**
     * Phase 4 (issue #39): semantic section label (INTRO/VERSE/PRE/HOOK/BRIDGE/OUTRO) so Ari
     * can target sections by name and the timeline can color-code regions.
     */
    @ColumnInfo(name = "label", defaultValue = "VERSE") val label: String = "VERSE",
    /**
     * Phase 4 (issue #39): per-section prompt memory — the Ari prompt that generated/defined
     * this section, editable inline in the DAW.
     */
    @ColumnInfo(name = "ari_prompt", defaultValue = "") val ariPrompt: String = ""
)

@Entity(tableName = "sound_genomes")
data class SoundGenomeEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val data: String
)

@Entity(tableName = "version_nodes")
data class VersionNodeEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val parentId: String?,
    val timestamp: Long
)

/**
 * Persisted [com.macsense.ai.audio.SoundArchive.Entry]. Genome and tags are stored as
 * serialized strings via [Converters] to keep the schema stable as SoundGenome evolves.
 */
@Entity(tableName = "sound_archive_entries")
data class SoundArchiveEntryEntity(
    @PrimaryKey val takeId: String,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "tags") val tags: String,
    @ColumnInfo(name = "genome_data") val genomeData: String?,
    @ColumnInfo(name = "origin_take_id") val originTakeId: String?
)

/**
 * A single audio clip/region placed on a section's timeline. This is the first concrete piece of
 * the Phase 2 "extend Room to a full track/clip/region schema" work item in
 * `PRODUCTION_HARDENING_PLAN.md`: previously the schema only tracked section *metadata* (name,
 * order, effect knob values in-memory) with no durable record of what audio actually lives where
 * on the timeline. A clip belongs to a section (a "track" in DAW terms is represented by [lane]
 * within a section, matching the existing 12-lane instrument grid naming used in `DawViewModel`),
 * references a take id already tracked in [SoundArchiveEntryEntity]/[PcmFileStore] rather than
 * duplicating raw audio into the DB, and stores its own trim/offset/gain so a single take can be
 * placed multiple times or trimmed differently across clips without re-recording it.
 *
 * `CASCADE` on the section foreign key means deleting a section also deletes its clips, since a
 * clip has no meaning without the section it's arranged into.
 */
@Entity(
    tableName = "clips",
    foreignKeys = [
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["section_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["section_id"]), Index(value = ["take_id"])]
)
data class ClipEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "section_id") val sectionId: String,
    @ColumnInfo(name = "lane") val lane: String,
    @ColumnInfo(name = "take_id") val takeId: String,
    @ColumnInfo(name = "start_frame") val startFrame: Long,
    @ColumnInfo(name = "trim_start_frame", defaultValue = "0") val trimStartFrame: Long = 0L,
    @ColumnInfo(name = "trim_end_frame") val trimEndFrame: Long?,
    @ColumnInfo(name = "gain_db", defaultValue = "0.0") val gainDb: Float = 0f,
    @ColumnInfo(name = "muted", defaultValue = "0") val muted: Boolean = false
)
