package com.macsense.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MacSenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects ORDER BY updated_at DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    // P8 (issue #43): sync bookkeeping
    @Query("SELECT * FROM projects WHERE is_dirty = 1")
    suspend fun getDirtyProjects(): List<ProjectEntity>

    @Query("UPDATE projects SET cloud_id = :cloudId, last_synced = :syncedAt, is_dirty = 0 WHERE id = :id")
    suspend fun markProjectSynced(id: String, cloudId: String, syncedAt: Long)

    @Query("UPDATE projects SET is_dirty = 1 WHERE id = :id")
    suspend fun markProjectDirty(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoundArchiveEntry(entry: SoundArchiveEntryEntity)

    @Query("SELECT * FROM sound_archive_entries")
    suspend fun getAllSoundArchiveEntries(): List<SoundArchiveEntryEntity>

    @Query("SELECT * FROM sound_archive_entries")
    fun observeSoundArchiveEntries(): Flow<List<SoundArchiveEntryEntity>>

    @Query("SELECT * FROM sound_archive_entries WHERE takeId = :takeId")
    suspend fun getSoundArchiveEntryByTakeId(takeId: String): SoundArchiveEntryEntity?

    @Query("DELETE FROM sound_archive_entries WHERE takeId = :takeId")
    suspend fun deleteSoundArchiveEntry(takeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoundGenome(genome: SoundGenomeEntity)

    @Query("SELECT * FROM sound_genomes WHERE id = :id")
    suspend fun getSoundGenomeById(id: String): SoundGenomeEntity?

    @Query("SELECT * FROM sound_genomes WHERE projectId = :projectId")
    suspend fun getSoundGenomesForProject(projectId: String): List<SoundGenomeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipEntity)

    @Query("SELECT * FROM clips WHERE section_id = :sectionId ORDER BY start_frame ASC")
    suspend fun getClipsForSection(sectionId: String): List<ClipEntity>

    @Query("SELECT * FROM clips WHERE section_id = :sectionId ORDER BY start_frame ASC")
    fun observeClipsForSection(sectionId: String): Flow<List<ClipEntity>>

    @Query("SELECT * FROM clips WHERE id = :id")
    suspend fun getClipById(id: String): ClipEntity?

    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun deleteClip(id: String)

    @Query("DELETE FROM clips WHERE section_id = :sectionId")
    suspend fun deleteClipsForSection(sectionId: String)

    // --- Phase 4 (issue #39): sections with semantic labels + prompt memory ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSection(section: SectionEntity)

    @Query("SELECT * FROM sections WHERE projectId = :projectId ORDER BY orderIndex ASC")
    suspend fun getSectionsForProject(projectId: String): List<SectionEntity>

    @Query("UPDATE sections SET ari_prompt = :prompt WHERE id = :sectionId")
    suspend fun updateSectionAriPrompt(sectionId: String, prompt: String)

    // --- Phase 4 (issue #39): A/B version branching nodes ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersionNode(node: VersionNodeEntity)

    @Query("SELECT * FROM version_nodes WHERE projectId = :projectId ORDER BY timestamp ASC")
    suspend fun getVersionNodesForProject(projectId: String): List<VersionNodeEntity>
}
