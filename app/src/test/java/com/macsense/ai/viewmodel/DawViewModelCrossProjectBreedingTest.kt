package com.macsense.ai.viewmodel

import com.macsense.ai.audio.SoundArchive
import com.macsense.ai.audio.SoundGenome
import com.macsense.ai.data.local.ClipEntity
import com.macsense.ai.data.local.MacSenseDao
import com.macsense.ai.data.local.ProjectEntity
import com.macsense.ai.data.local.SectionEntity
import com.macsense.ai.data.local.SoundArchiveEntryEntity
import com.macsense.ai.data.local.SoundGenomeEntity
import com.macsense.ai.data.local.VersionNodeEntity
import com.macsense.ai.data.repository.MacSenseRepository
import com.macsense.ai.export.GenomeArtifactCodec
import com.macsense.ai.ui.viewmodel.DawViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * P5+ (#61): end-to-end cross-project genome breeding through the ViewModel — export a Sound
 * DNA artifact from one "project" (repository), import it into another, breed it against a
 * local sound, and assert cross-project ancestry persists.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DawViewModelCrossProjectBreedingTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class InMemoryDao : MacSenseDao {
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
        }
        override suspend fun insertSoundGenome(genome: SoundGenomeEntity) {
            genomes.removeIf { it.id == genome.id }
            genomes.add(genome)
        }
        override suspend fun getSoundGenomeById(id: String) = genomes.find { it.id == id }
        override suspend fun getSoundGenomesForProject(projectId: String) =
            genomes.filter { it.projectId == projectId }
        override suspend fun insertClip(clip: ClipEntity) = Unit
        override suspend fun getClipsForSection(sectionId: String): List<ClipEntity> = emptyList()
        override fun observeClipsForSection(sectionId: String) = clipsFlow.asStateFlow()
        override suspend fun getClipById(id: String): ClipEntity? = null
        override suspend fun deleteClip(id: String) = Unit
        override suspend fun deleteClipsForSection(sectionId: String) = Unit
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun genome(sourceId: String) = SoundGenome(
        sourceId = sourceId,
        transient = 0.6,
        harmonicity = 0.5,
        brightness = 0.8,
        dynamics = 0.4,
        stereoWidth = 0.2,
        confidence = 0.9,
    )

    private suspend fun seed(repo: MacSenseRepository, takeId: String) {
        repo.upsertArchiveEntry(
            SoundArchive.Entry(takeId = takeId, state = SoundArchive.State.LIVING, genome = genome(takeId))
        )
    }

    private suspend fun <T : Any> await(what: String, timeoutMs: Long = 5000, get: () -> T?): T {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            get()?.let { return it }
            kotlinx.coroutines.delay(20)
        }
        throw AssertionError("Timed out waiting for $what")
    }

    @Test
    fun `export from project A, import into project B, breed with local sound`() = kotlinx.coroutines.runBlocking {
        // Project A exports.
        val repoA = MacSenseRepository(InMemoryDao())
        seed(repoA, "remote-take")
        val vmA = DawViewModel(repository = repoA, genomeProjectId = "project-a")
        vmA.exportGenomeArtifact("remote-take", trackName = "Night Drive", creatorName = "sia", now = 7L)
        val artifact = await("Sound DNA artifact") { vmA.lastExportedArtifact.value }

        // Project B imports and breeds against a local take.
        val repoB = MacSenseRepository(InMemoryDao())
        seed(repoB, "local-take")
        val vmB = DawViewModel(repository = repoB, genomeProjectId = "project-b")
        vmB.importGenomeArtifact(artifact, newTakeId = "imported-1")

        val imported = await("imported entry") { vmB.lastImportedEntry.value }
        assertTrue(GenomeArtifactCodec.IMPORTED_TAG in imported.tags)
        assertEquals("remote-take", imported.genome!!.sourceId)

        vmB.breedSoundsFromUi("local-take", "imported-1", traitBias = 0.5)
        val child = await("bred child") { vmB.lastBredEntry.value }
        val parents = child.genome!!.parents
        assertTrue("child must record the local parent", "local-take" in parents)
        assertTrue(
            "child must record the cross-project parent's genome identity",
            "remote-take" in parents || "imported-1" in parents
        )
        // Persisted, not just in-memory.
        assertNotNull(repoB.getArchiveEntryByTakeId(child.takeId))
    }

    @Test
    fun `importing an invalid artifact fails loudly and stores nothing`() = kotlinx.coroutines.runBlocking {
        val repo = MacSenseRepository(InMemoryDao())
        val vm = DawViewModel(repository = repo, genomeProjectId = "p")
        vm.importGenomeArtifact("garbage", newTakeId = "x")
        kotlinx.coroutines.delay(300)
        assertNull(vm.lastImportedEntry.value)
        assertNull(repo.getArchiveEntryByTakeId("x"))
    }
}
