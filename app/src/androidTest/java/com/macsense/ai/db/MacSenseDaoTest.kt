package com.macsense.ai.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.macsense.ai.audio.SoundArchive
import com.macsense.ai.audio.SoundGenome
import com.macsense.ai.data.local.MacSenseDatabase
import com.macsense.ai.data.local.ProjectEntity
import com.macsense.ai.data.local.SoundArchiveEntryEntity
import com.macsense.ai.data.local.SoundGenomeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MacSenseDaoTest {
    private lateinit var db: MacSenseDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MacSenseDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetProject() = runBlocking {
        val project = ProjectEntity("p1", "Test", 0L, 0L, 120.0)
        db.dao().insertProject(project)
        val loaded = db.dao().getProjectById("p1")
        assertEquals(project.name, loaded?.name)
    }

    @Test
    fun insertAndGetSoundArchiveEntry() = runBlocking {
        val genome = SoundGenome(
            sourceId = "take1",
            transient = 0.5,
            harmonicity = 0.4,
            brightness = 0.6,
            dynamics = 0.3
        )
        val entry = SoundArchiveEntryEntity(
            takeId = "take1",
            state = SoundArchive.State.LIVING.name,
            tags = "vocal,lead",
            genomeData = Json.encodeToString(genome),
            originTakeId = null
        )
        db.dao().insertSoundArchiveEntry(entry)

        val loaded = db.dao().getSoundArchiveEntryByTakeId("take1")
        assertEquals(entry.state, loaded?.state)
        assertEquals(entry.tags, loaded?.tags)
        assertEquals(entry.genomeData, loaded?.genomeData)
    }

    @Test
    fun insertSoundArchiveEntry_replacesOnConflict() = runBlocking {
        db.dao().insertSoundArchiveEntry(
            SoundArchiveEntryEntity("take1", SoundArchive.State.LIVING.name, "", null, null)
        )
        db.dao().insertSoundArchiveEntry(
            SoundArchiveEntryEntity("take1", SoundArchive.State.DORMANT.name, "tag1", null, null)
        )

        val all = db.dao().getAllSoundArchiveEntries()
        assertEquals(1, all.size)
        assertEquals(SoundArchive.State.DORMANT.name, all.first().state)
    }

    @Test
    fun deleteSoundArchiveEntry_removesRow() = runBlocking {
        db.dao().insertSoundArchiveEntry(
            SoundArchiveEntryEntity("take1", SoundArchive.State.LIVING.name, "", null, null)
        )
        db.dao().deleteSoundArchiveEntry("take1")

        assertNull(db.dao().getSoundArchiveEntryByTakeId("take1"))
    }

    @Test
    fun observeSoundArchiveEntries_emitsInsertedRow() = runBlocking {
        db.dao().insertSoundArchiveEntry(
            SoundArchiveEntryEntity("take1", SoundArchive.State.LIVING.name, "", null, null)
        )

        val observed = db.dao().observeSoundArchiveEntries().first()
        assertEquals(1, observed.size)
        assertEquals("take1", observed.first().takeId)
    }

    @Test
    fun insertAndGetSoundGenome() = runBlocking {
        val genomeEntity = SoundGenomeEntity(id = "g1", projectId = "proj1", data = "{}")
        db.dao().insertSoundGenome(genomeEntity)

        val loaded = db.dao().getSoundGenomeById("g1")
        assertEquals("proj1", loaded?.projectId)
    }

    @Test
    fun getSoundGenomesForProject_filtersByProjectId() = runBlocking {
        db.dao().insertSoundGenome(SoundGenomeEntity("g1", "proj1", "{}"))
        db.dao().insertSoundGenome(SoundGenomeEntity("g2", "proj1", "{}"))
        db.dao().insertSoundGenome(SoundGenomeEntity("g3", "proj2", "{}"))

        val proj1Genomes = db.dao().getSoundGenomesForProject("proj1")
        assertEquals(2, proj1Genomes.size)
        assertTrue(proj1Genomes.all { it.projectId == "proj1" })
    }
}
