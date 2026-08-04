package com.macsense.ai.ui.viewmodel

import com.macsense.ai.data.local.MacSenseDao
import com.macsense.ai.data.local.ProjectEntity
import com.macsense.ai.data.local.SoundArchiveEntryEntity
import com.macsense.ai.data.local.SoundGenomeEntity
import com.macsense.ai.data.repository.MacSenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DawViewModelFactoryTest {

    private class NoOpDao : MacSenseDao {
        private val archiveFlow = MutableStateFlow<List<SoundArchiveEntryEntity>>(emptyList())
        override suspend fun insertProject(project: ProjectEntity) {}
        override suspend fun getProjectById(id: String): ProjectEntity? = null
        override fun getAllProjects() = flowOf(emptyList<ProjectEntity>())
        override suspend fun deleteProject(id: String) {}
        override suspend fun insertSoundArchiveEntry(entry: SoundArchiveEntryEntity) {}
        override suspend fun getAllSoundArchiveEntries(): List<SoundArchiveEntryEntity> = emptyList()
        override fun observeSoundArchiveEntries() = archiveFlow.asStateFlow()
        override suspend fun getSoundArchiveEntryByTakeId(takeId: String): SoundArchiveEntryEntity? = null
        override suspend fun deleteSoundArchiveEntry(takeId: String) {}
        override suspend fun insertSoundGenome(genome: SoundGenomeEntity) {}
        override suspend fun getSoundGenomeById(id: String): SoundGenomeEntity? = null
        override suspend fun getSoundGenomesForProject(projectId: String): List<SoundGenomeEntity> = emptyList()
    }

    @Test
    fun create_returnsDawViewModelWithRepositoryWired() {
        val repository = MacSenseRepository(NoOpDao())
        val factory = DawViewModelFactory(repository)

        val vm = factory.create(DawViewModel::class.java)

        assertNotNull(vm)
    }

    @Test
    fun create_throwsForUnrelatedViewModelClass() {
        val repository = MacSenseRepository(NoOpDao())
        val factory = DawViewModelFactory(repository)

        assertThrows(IllegalArgumentException::class.java) {
            factory.create(MasteringViewModel::class.java)
        }
    }
}
