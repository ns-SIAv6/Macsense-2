package com.macsense.ai.sync

import com.macsense.ai.data.local.ClipEntity
import com.macsense.ai.data.local.MacSenseDao
import com.macsense.ai.data.local.ProjectEntity
import com.macsense.ai.data.local.SectionEntity
import com.macsense.ai.data.local.SoundArchiveEntryEntity
import com.macsense.ai.data.local.SoundGenomeEntity
import com.macsense.ai.data.local.VersionNodeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectSyncEngineTest {

    private class FakeDao : MacSenseDao {
        val projects = mutableListOf<ProjectEntity>()
        override suspend fun insertProject(project: ProjectEntity) {
            projects.removeIf { it.id == project.id }
            projects.add(project)
        }
        override suspend fun getProjectById(id: String) = projects.find { it.id == id }
        override fun getAllProjects() = flowOf(projects.toList())
        override suspend fun deleteProject(id: String) { projects.removeIf { it.id == id } }
        override suspend fun getDirtyProjects() = projects.filter { it.isDirty }
        override suspend fun markProjectSynced(id: String, cloudId: String, syncedAt: Long) {
            val p = projects.find { it.id == id } ?: return
            insertProject(p.copy(cloudId = cloudId, lastSynced = syncedAt, isDirty = false))
        }
        override suspend fun markProjectDirty(id: String) {
            val p = projects.find { it.id == id } ?: return
            insertProject(p.copy(isDirty = true))
        }

        override suspend fun upsertSection(section: SectionEntity) = Unit
        override suspend fun getSectionsForProject(projectId: String): List<SectionEntity> = emptyList()
        override suspend fun updateSectionAriPrompt(sectionId: String, prompt: String) = Unit
        override suspend fun insertVersionNode(node: VersionNodeEntity) = Unit
        override suspend fun getVersionNodesForProject(projectId: String): List<VersionNodeEntity> = emptyList()
        override suspend fun insertSoundArchiveEntry(entry: SoundArchiveEntryEntity) = Unit
        override suspend fun getAllSoundArchiveEntries(): List<SoundArchiveEntryEntity> = emptyList()
        override fun observeSoundArchiveEntries() = MutableStateFlow(emptyList<SoundArchiveEntryEntity>()).asStateFlow()
        override suspend fun getSoundArchiveEntryByTakeId(takeId: String): SoundArchiveEntryEntity? = null
        override suspend fun deleteSoundArchiveEntry(takeId: String) = Unit
        override suspend fun insertSoundGenome(genome: SoundGenomeEntity) = Unit
        override suspend fun getSoundGenomeById(id: String): SoundGenomeEntity? = null
        override suspend fun getSoundGenomesForProject(projectId: String): List<SoundGenomeEntity> = emptyList()
        override suspend fun insertClip(clip: ClipEntity) = Unit
        override suspend fun getClipsForSection(sectionId: String): List<ClipEntity> = emptyList()
        override fun observeClipsForSection(sectionId: String) = MutableStateFlow(emptyList<ClipEntity>()).asStateFlow()
        override suspend fun getClipById(id: String): ClipEntity? = null
        override suspend fun deleteClip(id: String) = Unit
        override suspend fun deleteClipsForSection(sectionId: String) = Unit
    }

    private class FakeRemote : SupabaseSyncRemote {
        val cloud = mutableMapOf<String, CloudProject>()
        var failNext = false
        override suspend fun upsertProject(project: CloudProject): CloudProject {
            if (failNext) throw java.io.IOException("network down")
            val stored = project.copy(id = project.id ?: "cloud-${project.localId}")
            cloud[project.localId] = stored
            return stored
        }
        override suspend fun fetchProject(localId: String): CloudProject? {
            if (failNext) throw java.io.IOException("network down")
            return cloud[localId]
        }
    }

    private fun project(id: String, updatedAt: Long, dirty: Boolean = true) = ProjectEntity(
        id = id, name = "P$id", createdAt = 1L, updatedAt = updatedAt, bpm = 120.0, isDirty = dirty,
    )

    @Test
    fun `dirty projects upload and are marked clean with a cloud id`() = runBlocking {
        val dao = FakeDao()
        val remote = FakeRemote()
        dao.insertProject(project("a", updatedAt = 100))
        dao.insertProject(project("b", updatedAt = 200, dirty = false))

        val result = ProjectSyncEngine(dao, remote, now = { 999L }).syncDirtyProjects()

        assertEquals(1, result.uploaded)
        assertEquals(0, result.failed)
        val synced = dao.getProjectById("a")!!
        assertFalse(synced.isDirty)
        assertEquals("cloud-a", synced.cloudId)
        assertEquals(999L, synced.lastSynced)
        // Clean project untouched.
        assertNull(dao.getProjectById("b")!!.cloudId)
    }

    @Test
    fun `network failure keeps the project dirty - no data loss`() = runBlocking {
        val dao = FakeDao()
        val remote = FakeRemote().apply { failNext = true }
        dao.insertProject(project("a", updatedAt = 100))

        val result = ProjectSyncEngine(dao, remote).syncDirtyProjects()

        assertEquals(0, result.uploaded)
        assertEquals(1, result.failed)
        assertTrue(dao.getProjectById("a")!!.isDirty)

        // Connection returns: retry succeeds.
        remote.failNext = false
        val retry = ProjectSyncEngine(dao, remote, now = { 5L }).syncDirtyProjects()
        assertEquals(1, retry.uploaded)
        assertFalse(dao.getProjectById("a")!!.isDirty)
    }

    @Test
    fun `newer cloud copy wins and conflict is recorded`() = runBlocking {
        val dao = FakeDao()
        val remote = FakeRemote()
        remote.cloud["a"] = CloudProject(
            id = "cloud-a", localId = "a", name = "newer", bpm = 90.0,
            createdAtMs = 1L, updatedAtMs = 500L,
        )
        dao.insertProject(project("a", updatedAt = 100))

        val result = ProjectSyncEngine(dao, remote, now = { 7L }).syncDirtyProjects()

        assertEquals(0, result.uploaded)
        assertEquals(1, result.skippedConflicts)
        val p = dao.getProjectById("a")!!
        assertFalse(p.isDirty)
        assertEquals("cloud-a", p.cloudId)
        // Cloud row untouched — cloud won.
        assertEquals("newer", remote.cloud["a"]!!.name)
    }

    @Test
    fun `local newer than cloud overwrites cloud (last write wins)`() = runBlocking {
        val dao = FakeDao()
        val remote = FakeRemote()
        remote.cloud["a"] = CloudProject(
            id = "cloud-a", localId = "a", name = "old", bpm = 90.0,
            createdAtMs = 1L, updatedAtMs = 50L,
        )
        dao.insertProject(project("a", updatedAt = 100).copy(cloudId = "cloud-a"))

        val result = ProjectSyncEngine(dao, remote).syncDirtyProjects()

        assertEquals(1, result.uploaded)
        assertEquals("Pa", remote.cloud["a"]!!.name)
    }
}
