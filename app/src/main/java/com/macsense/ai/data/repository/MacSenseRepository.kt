package com.macsense.ai.data.repository

import com.macsense.ai.audio.SoundArchive
import com.macsense.ai.audio.SoundGenome
import com.macsense.ai.data.local.ClipEntity
import com.macsense.ai.data.local.Converters
import com.macsense.ai.data.local.MacSenseDao
import com.macsense.ai.data.local.ProjectEntity
import com.macsense.ai.data.local.SectionEntity
import com.macsense.ai.data.local.VersionNodeEntity
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

    /** Places or updates a clip on a section's timeline. See [ClipEntity] for field semantics. */
    suspend fun upsertClip(clip: ClipEntity) {
        dao.insertClip(clip)
    }

    suspend fun getClipsForSection(sectionId: String): List<ClipEntity> =
        dao.getClipsForSection(sectionId)

    fun observeClipsForSection(sectionId: String): Flow<List<ClipEntity>> =
        dao.observeClipsForSection(sectionId)

    suspend fun getClipById(id: String): ClipEntity? = dao.getClipById(id)

    suspend fun deleteClip(id: String) {
        dao.deleteClip(id)
    }

    suspend fun deleteClipsForSection(sectionId: String) {
        dao.deleteClipsForSection(sectionId)
    }


    // --- Phase 4 (issue #39): sections + version branching ---

    suspend fun upsertSection(section: SectionEntity) {
        dao.upsertSection(section)
    }

    suspend fun getSectionsForProject(projectId: String): List<SectionEntity> =
        dao.getSectionsForProject(projectId)

    suspend fun updateSectionAriPrompt(sectionId: String, prompt: String) {
        dao.updateSectionAriPrompt(sectionId, prompt)
    }

    suspend fun insertVersionNode(node: VersionNodeEntity) {
        dao.insertVersionNode(node)
    }

    suspend fun getVersionNodesForProject(projectId: String): List<VersionNodeEntity> =
        dao.getVersionNodesForProject(projectId)

    private fun SoundArchiveEntryEntity.toDomain(): SoundArchive.Entry = SoundArchive.Entry(
        takeId = takeId,
        state = converters.toArchiveState(state) ?: SoundArchive.State.LIVING,
        tags = converters.toStringSet(tags),
        genome = converters.toSoundGenome(genomeData),
        originTakeId = originTakeId
    )
}
