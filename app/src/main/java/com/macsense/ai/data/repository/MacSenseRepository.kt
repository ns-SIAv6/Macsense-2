package com.macsense.ai.data.repository

import com.macsense.ai.audio.SoundArchive
import com.macsense.ai.audio.SoundGenome
import com.macsense.ai.data.local.Converters
import com.macsense.ai.data.local.MacSenseDao
import com.macsense.ai.data.local.ProjectEntity
import com.macsense.ai.data.local.SoundArchiveEntryEntity
import com.macsense.ai.data.local.SoundGenomeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MacSenseRepository(private val dao: MacSenseDao) {
    private val converters = Converters()

    fun getAllProjects(): Flow<List<ProjectEntity>> = dao.getAllProjects()

    suspend fun insertProject(project: ProjectEntity) {
        dao.insertProject(project)
    }

    suspend fun upsertArchiveEntry(entry: SoundArchive.Entry) {
        dao.insertSoundArchiveEntry(
            SoundArchiveEntryEntity(
                takeId = entry.takeId,
                state = converters.fromArchiveState(entry.state) ?: SoundArchive.State.LIVING.name,
                tags = converters.fromStringSet(entry.tags),
                genomeData = converters.fromSoundGenome(entry.genome),
                originTakeId = entry.originTakeId
            )
        )
    }

    suspend fun getArchiveEntries(): List<SoundArchive.Entry> =
        dao.getAllSoundArchiveEntries().map { it.toDomain() }

    fun observeArchiveEntries(): Flow<List<SoundArchive.Entry>> =
        dao.observeSoundArchiveEntries().map { list -> list.map { it.toDomain() } }

    suspend fun getArchiveEntryByTakeId(takeId: String): SoundArchive.Entry? =
        dao.getSoundArchiveEntryByTakeId(takeId)?.toDomain()

    suspend fun deleteArchiveEntry(takeId: String) {
        dao.deleteSoundArchiveEntry(takeId)
    }

    suspend fun upsertSoundGenome(projectId: String, genome: SoundGenome) {
        dao.insertSoundGenome(
            SoundGenomeEntity(
                id = genome.sourceId,
                projectId = projectId,
                data = converters.fromSoundGenome(genome).orEmpty()
            )
        )
    }

    suspend fun getSoundGenome(id: String): SoundGenome? =
        dao.getSoundGenomeById(id)?.let { converters.toSoundGenome(it.data) }

    suspend fun getSoundGenomesForProject(projectId: String): List<SoundGenome> =
        dao.getSoundGenomesForProject(projectId).mapNotNull { converters.toSoundGenome(it.data) }

    private fun SoundArchiveEntryEntity.toDomain(): SoundArchive.Entry = SoundArchive.Entry(
        takeId = takeId,
        state = converters.toArchiveState(state) ?: SoundArchive.State.LIVING,
        tags = converters.toStringSet(tags),
        genome = converters.toSoundGenome(genomeData),
        originTakeId = originTakeId
    )
}
