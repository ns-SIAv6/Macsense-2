package com.macsense.ai.repository

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MacSenseRepositoryTest {

    private class FakeDao : MacSenseDao {
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

    private lateinit var dao: FakeDao
    private lateinit var repo: MacSenseRepository

    @Before
    fun setUp() {
        dao = FakeDao()
        repo = MacSenseRepository(dao)
    }

    @Test
    fun testRepository() = runBlocking {
        repo.insertProject(ProjectEntity("1", "test", 0L, 0L, 120.0))
        var count = 0
        repo.getAllProjects().collect { count = it.size }
        assertEquals(1, count)
    }

    @Test
    fun upsertAndGetArchiveEntry_roundTrips() = runBlocking {
        val genome = SoundGenome(
            sourceId = "take1",
            transient = 0.5,
            harmonicity = 0.4,
            brightness = 0.6,
            dynamics = 0.3
        )
        val entry = SoundArchive.Entry(
            takeId = "take1",
            state = SoundArchive.State.LIVING,
            tags = setOf("vocal", "lead"),
            genome = genome,
            originTakeId = null
        )

        repo.upsertArchiveEntry(entry)
        val loaded = repo.getArchiveEntryByTakeId("take1")

        assertEquals(entry.takeId, loaded?.takeId)
        assertEquals(entry.state, loaded?.state)
        assertEquals(entry.tags, loaded?.tags)
        assertEquals(entry.genome, loaded?.genome)
    }

    @Test
    fun upsertArchiveEntry_replacesExistingEntryForSameTakeId() = runBlocking {
        repo.upsertArchiveEntry(SoundArchive.Entry(takeId = "take1", state = SoundArchive.State.LIVING))
        repo.upsertArchiveEntry(SoundArchive.Entry(takeId = "take1", state = SoundArchive.State.DORMANT))

        val all = repo.getArchiveEntries()
        assertEquals(1, all.size)
        assertEquals(SoundArchive.State.DORMANT, all.first().state)
    }

    @Test
    fun deleteArchiveEntry_removesEntry() = runBlocking {
        repo.upsertArchiveEntry(SoundArchive.Entry(takeId = "take1"))
        repo.deleteArchiveEntry("take1")

        assertNull(repo.getArchiveEntryByTakeId("take1"))
    }

    @Test
    fun observeArchiveEntries_mapsEntitiesToDomain() = runBlocking {
        repo.upsertArchiveEntry(SoundArchive.Entry(takeId = "take1", state = SoundArchive.State.REBORN))

        val observed = repo.observeArchiveEntries().first()
        assertEquals(1, observed.size)
        assertEquals(SoundArchive.State.REBORN, observed.first().state)
    }

    @Test
    fun upsertAndGetSoundGenome_roundTrips() = runBlocking {
        val genome = SoundGenome(
            sourceId = "g1",
            transient = 0.1,
            harmonicity = 0.2,
            brightness = 0.3,
            dynamics = 0.4
        )

        repo.upsertSoundGenome("proj1", genome)
        val loaded = repo.getSoundGenome("g1")

        assertEquals(genome, loaded)
    }

    @Test
    fun getSoundGenomesForProject_returnsOnlyMatchingProject() = runBlocking {
        repo.upsertSoundGenome("proj1", SoundGenome("g1", 0.1, 0.1, 0.1, 0.1))
        repo.upsertSoundGenome("proj1", SoundGenome("g2", 0.2, 0.2, 0.2, 0.2))
        repo.upsertSoundGenome("proj2", SoundGenome("g3", 0.3, 0.3, 0.3, 0.3))

        val proj1Genomes = repo.getSoundGenomesForProject("proj1")
        assertEquals(2, proj1Genomes.size)
        assertTrue(proj1Genomes.all { it.sourceId in setOf("g1", "g2") })
    }

    @Test
    fun upsertAndGetClipsForSection_roundTripsInStartFrameOrder() = runBlocking {
        val clipA = ClipEntity(
            id = "clipA", sectionId = "verse1", lane = "Kick", takeId = "take1",
            startFrame = 44_100L, trimEndFrame = null
        )
        val clipB = ClipEntity(
            id = "clipB", sectionId = "verse1", lane = "Snare", takeId = "take2",
            startFrame = 0L, trimEndFrame = 22_050L
        )

        repo.upsertClip(clipA)
        repo.upsertClip(clipB)

        val clips = repo.getClipsForSection("verse1")
        assertEquals(2, clips.size)
        assertEquals("clipB", clips.first().id) // startFrame 0 sorts before 44_100
        assertEquals("clipA", clips.last().id)
    }

    @Test
    fun deleteClipsForSection_removesOnlyThatSectionsClips() = runBlocking {
        repo.upsertClip(ClipEntity(id = "c1", sectionId = "verse1", lane = "Kick", takeId = "t1", startFrame = 0L, trimEndFrame = null))
        repo.upsertClip(ClipEntity(id = "c2", sectionId = "hook", lane = "Kick", takeId = "t2", startFrame = 0L, trimEndFrame = null))

        repo.deleteClipsForSection("verse1")

        assertTrue(repo.getClipsForSection("verse1").isEmpty())
        assertEquals(1, repo.getClipsForSection("hook").size)
    }
}
