package com.macsense.ai.viewmodel

import com.macsense.ai.api.AriCommand
import com.macsense.ai.audio.SoundArchive
import com.macsense.ai.audio.SoundGenome
import com.macsense.ai.data.local.ClipEntity
import com.macsense.ai.data.local.MacSenseDao
import com.macsense.ai.data.local.VersionNodeEntity
import com.macsense.ai.data.local.SectionEntity
import com.macsense.ai.data.local.ProjectEntity
import com.macsense.ai.data.local.SoundArchiveEntryEntity
import com.macsense.ai.data.local.SoundGenomeEntity
import com.macsense.ai.data.repository.MacSenseRepository
import com.macsense.ai.ui.viewmodel.DawViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * End-to-end tests for the Phase 5 sound-genetics Ari commands (`breed_sounds` /
 * `resurrect_sound`) wired through [DawViewModel.applyAriCommand], asserting that the
 * resulting archive entries actually persist through [MacSenseRepository] rather than
 * just updating in-memory StateFlows.
 *
 * Mirrors the [FakeDao] pattern used in `MacSenseRepositoryTest`, backed by an
 * [UnconfinedTestDispatcher] so `viewModelScope.launch(Dispatchers.IO/Main)` coroutines
 * inside [DawViewModel] run synchronously within each test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DawViewModelGeneticsPersistenceTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeMacSenseRepositoryDao : MacSenseDao {
        override suspend fun getDirtyProjects(): List<ProjectEntity> = emptyList()
        override suspend fun markProjectSynced(id: String, cloudId: String, syncedAt: Long) = Unit
        override suspend fun markProjectDirty(id: String) = Unit
        override suspend fun upsertSection(section: SectionEntity) = Unit
        override suspend fun getSectionsForProject(projectId: String): List<SectionEntity> = emptyList()
        override suspend fun updateSectionAriPrompt(sectionId: String, prompt: String) = Unit
        override suspend fun insertVersionNode(node: VersionNodeEntity) = Unit
        override suspend fun getVersionNodesForProject(projectId: String): List<VersionNodeEntity> = emptyList()

        val projects = mutableListOf<ProjectEntity>()
        val archiveEntries = mutableListOf<SoundArchiveEntryEntity>()
        val genomes = mutableListOf<SoundGenomeEntity>()
        val clips = mutableListOf<ClipEntity>()
        private val archiveFlow = MutableStateFlow<List<SoundArchiveEntryEntity>>(emptyList())
        private val clipsFlow = MutableStateFlow<List<ClipEntity>>(emptyList())

        override suspend fun insertProject(project: ProjectEntity) { projects.add(project) }
        override suspend fun getProjectById(id: String) = projects.find { it.id == id }
        override fun getAllProjects() = flowOf(projects)
        override suspend fun deleteProject(id: String) { projects.removeIf { it.id == id } }

        override suspend fun insertSoundArchiveEntry(entry: SoundArchiveEntryEntity) {
            archiveEntries.removeIf { it.takeId == entry.takeId }
            archiveEntries.add(entry)
            archiveFlow.value = archiveEntries.toList()
        }

        override suspend fun getAllSoundArchiveEntries() = archiveEntries.toList()
        override fun observeSoundArchiveEntries() = archiveFlow.asStateFlow()
        override suspend fun getSoundArchiveEntryByTakeId(takeId: String) =
            archiveEntries.find { it.takeId == takeId }

        override suspend fun deleteSoundArchiveEntry(takeId: String) {
            archiveEntries.removeIf { it.takeId == takeId }
            archiveFlow.value = archiveEntries.toList()
        }

        override suspend fun insertSoundGenome(genome: SoundGenomeEntity) {
            genomes.removeIf { it.id == genome.id }
            genomes.add(genome)
        }

        override suspend fun getSoundGenomeById(id: String) = genomes.find { it.id == id }
        override suspend fun getSoundGenomesForProject(projectId: String) =
            genomes.filter { it.projectId == projectId }

        override suspend fun insertClip(clip: ClipEntity) {
            clips.removeIf { it.id == clip.id }
            clips.add(clip)
            clipsFlow.value = clips.toList()
        }

        override suspend fun getClipsForSection(sectionId: String) =
            clips.filter { it.sectionId == sectionId }.sortedBy { it.startFrame }

        override fun observeClipsForSection(sectionId: String) = clipsFlow.asStateFlow()

        override suspend fun getClipById(id: String) = clips.find { it.id == id }

        override suspend fun deleteClip(id: String) {
            clips.removeIf { it.id == id }
            clipsFlow.value = clips.toList()
        }

        override suspend fun deleteClipsForSection(sectionId: String) {
            clips.removeIf { it.sectionId == sectionId }
            clipsFlow.value = clips.toList()
        }
    }

    private lateinit var dao: FakeMacSenseRepositoryDao
    private lateinit var repository: MacSenseRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        dao = FakeMacSenseRepositoryDao()
        repository = MacSenseRepository(dao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun genome(sourceId: String) = SoundGenome(
        sourceId = sourceId,
        transient = 0.5,
        harmonicity = 0.4,
        brightness = 0.6,
        dynamics = 0.3
    )

    private suspend fun seedLivingEntry(takeId: String) {
        val g = genome(takeId)
        repository.upsertSoundGenome("default-project", g)
        repository.upsertArchiveEntry(
            SoundArchive.Entry(
                takeId = takeId,
                state = SoundArchive.State.LIVING,
                tags = setOf("vocal"),
                genome = g,
                originTakeId = null
            )
        )
    }

    @Test
    fun breedSounds_persistsRebornEntryAndUpdatesLastBredEntry() = kotlinx.coroutines.test.runTest(dispatcher) {
        seedLivingEntry("take1")
        seedLivingEntry("take2")

        val vm = DawViewModel(repository = repository)

        val cmd = AriCommand(
            type = "breed_sounds",
            parent_take_id = "take1",
            parent_take_id_2 = "take2",
            trait_bias = 0.5,
            tags = listOf("experimental"),
            explanation = "crossing these two"
        )
        vm.applyAriCommand(cmd)

        var attempts = 0
        while (vm.lastBredEntry.value == null && attempts < 100) {
            delay(10)
            attempts++
        }

        val bred = vm.lastBredEntry.value
        assertNotNull("lastBredEntry should be set after a successful breed_sounds command", bred)
        assertEquals(SoundArchive.State.REBORN, bred?.state)

        val persisted = repository.getArchiveEntryByTakeId(requireNotNull(bred).takeId)
        assertNotNull("bred entry should actually be persisted in the repository", persisted)
        assertEquals(SoundArchive.State.REBORN, persisted?.state)
        assertNotNull("bred entry should carry a genome", persisted?.genome)

        val allEntries = repository.getArchiveEntries()
        assertEquals(3, allEntries.size) // take1, take2, plus the new offspring
    }

    @Test
    fun resurrectSound_persistsNewRebornEntryLinkedToOrigin() = kotlinx.coroutines.test.runTest(dispatcher) {
        seedLivingEntry("dormant1")

        val vm = DawViewModel(repository = repository)

        val cmd = AriCommand(
            type = "resurrect_sound",
            take_id = "dormant1",
            tags = listOf("revived"),
            explanation = "bringing it back"
        )
        vm.applyAriCommand(cmd)

        var attempts = 0
        while (vm.lastResurrectedEntry.value == null && attempts < 100) {
            delay(10)
            attempts++
        }

        val resurrected = vm.lastResurrectedEntry.value
        assertNotNull("lastResurrectedEntry should be set after a successful resurrect_sound command", resurrected)
        assertEquals(SoundArchive.State.REBORN, resurrected?.state)
        assertEquals("dormant1", resurrected?.originTakeId)
        assertTrue("tags should be unioned with the source entry's tags", resurrected?.tags?.containsAll(setOf("vocal", "revived")) == true)

        val persisted = repository.getArchiveEntryByTakeId(requireNotNull(resurrected).takeId)
        assertNotNull("resurrected entry should actually be persisted in the repository", persisted)
        assertEquals("dormant1", persisted?.originTakeId)

        val allEntries = repository.getArchiveEntries()
        assertEquals(2, allEntries.size) // original dormant1 plus the new reborn take
    }

    @Test
    fun breedSounds_isNoOpWhenRepositoryIsNull() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = DawViewModel() // no repository wired, matches constructor default

        val cmd = AriCommand(
            type = "breed_sounds",
            parent_take_id = "take1",
            parent_take_id_2 = "take2",
            trait_bias = 0.5,
            explanation = "should be a safe no-op"
        )
        vm.applyAriCommand(cmd)

        assertNull("lastBredEntry should stay null when no repository is wired", vm.lastBredEntry.value)
    }

    @Test
    fun resurrectSound_isNoOpWhenRepositoryIsNull() = kotlinx.coroutines.test.runTest(dispatcher) {
        val vm = DawViewModel() // no repository wired, matches constructor default

        val cmd = AriCommand(
            type = "resurrect_sound",
            take_id = "dormant1",
            explanation = "should be a safe no-op"
        )
        vm.applyAriCommand(cmd)

        assertNull("lastResurrectedEntry should stay null when no repository is wired", vm.lastResurrectedEntry.value)
    }
}
