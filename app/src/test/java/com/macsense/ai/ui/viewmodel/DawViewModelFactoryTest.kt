package com.macsense.ai.ui.viewmodel

import com.macsense.ai.data.local.ClipEntity
import com.macsense.ai.data.local.MacSenseDao
import com.macsense.ai.data.local.VersionNodeEntity
import com.macsense.ai.data.local.SectionEntity
import com.macsense.ai.data.local.ProjectEntity
import com.macsense.ai.data.local.SoundArchiveEntryEntity
import com.macsense.ai.data.local.SoundGenomeEntity
import com.macsense.ai.data.repository.MacSenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DawViewModelFactoryTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class NoOpDao : MacSenseDao {
        override suspend fun upsertSection(section: SectionEntity) = Unit
        override suspend fun getSectionsForProject(projectId: String): List<SectionEntity> = emptyList()
        override suspend fun updateSectionAriPrompt(sectionId: String, prompt: String) = Unit
        override suspend fun insertVersionNode(node: VersionNodeEntity) = Unit
        override suspend fun getVersionNodesForProject(projectId: String): List<VersionNodeEntity> = emptyList()

        private val archiveFlow = MutableStateFlow<List<SoundArchiveEntryEntity>>(emptyList())
        private val clipsFlow = MutableStateFlow<List<ClipEntity>>(emptyList())
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
        override suspend fun insertClip(clip: ClipEntity) {}
        override suspend fun getClipsForSection(sectionId: String): List<ClipEntity> = emptyList()
        override fun observeClipsForSection(sectionId: String) = clipsFlow.asStateFlow()
        override suspend fun getClipById(id: String): ClipEntity? = null
        override suspend fun deleteClip(id: String) {}
        override suspend fun deleteClipsForSection(sectionId: String) {}
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
