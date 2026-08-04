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
}
