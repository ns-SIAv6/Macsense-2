package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MacSenseDao {

    // --- Sound Genomes ---
    @Query("SELECT * FROM sound_genomes WHERE isExtinct = 0 ORDER BY generation DESC, name ASC")
    fun getAllActiveGenomes(): Flow<List<SoundGenomeEntity>>

    @Query("SELECT * FROM sound_genomes WHERE isExtinct = 1 ORDER BY deathTimestamp DESC")
    fun getExtinctGenomes(): Flow<List<SoundGenomeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenome(genome: SoundGenomeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGenomes(genomes: List<SoundGenomeEntity>)

    @Query("UPDATE sound_genomes SET isExtinct = 1, deathTimestamp = :deathTime, deathReason = :reason, epitaph = :epitaph WHERE id = :id")
    suspend fun markGenomeExtinct(id: String, deathTime: Long, reason: String, epitaph: String)

    // --- Projects & Tracks ---
    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("SELECT * FROM tracks WHERE projectId = :projectId")
    fun getTracksForProject(projectId: String): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    // --- Lyrics ---
    @Query("SELECT * FROM lyrics WHERE projectId = :projectId ORDER BY lineIndex ASC")
    fun getLyricsForProject(projectId: String): Flow<List<LyricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyric(lyric: LyricEntity)

    @Query("UPDATE lyrics SET text = :newText WHERE id = :id")
    suspend fun updateLyricText(id: String, newText: String)

    // --- Version Nodes ---
    @Query("SELECT * FROM version_nodes WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getVersionNodes(projectId: String): Flow<List<VersionNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersionNode(node: VersionNodeEntity)

    // --- Breeding History ---
    @Query("SELECT * FROM breeding_history ORDER BY timestamp DESC")
    fun getBreedingHistory(): Flow<List<BreedingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreedingHistory(history: BreedingHistoryEntity)

    // --- MIDI Mappings ---
    @Query("SELECT * FROM midi_mappings")
    fun getMidiMappings(): Flow<List<MidiMappingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMidiMapping(mapping: MidiMappingEntity)

}
