package com.macsense.ai.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.macsense.ai.audio.SoundArchive
import com.macsense.ai.audio.SoundGenome
import com.macsense.ai.data.local.ClipEntity
import com.macsense.ai.data.local.MacSenseDatabase
import com.macsense.ai.data.local.ProjectEntity
import com.macsense.ai.data.local.SectionEntity
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
        )
            // Foreign keys/cascade behavior aren't enforced by Room's in-memory builder by
            // default the way they are on-device; this matches production PRAGMA foreign_keys=ON.
            .build()
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

    private suspend fun seedSection(id: String = "verse1") {
        db.dao().insertProject(ProjectEntity("proj1", "Test", 0L, 0L, 120.0))
        db.dao().insertSoundArchiveEntry(
            SoundArchiveEntryEntity("take1", SoundArchive.State.LIVING.name, "", null, null)
        )
        // SectionEntity has no DAO insert method exposed yet (sections are still managed
        // in-memory by DawViewModel per PRODUCTION_HARDENING_PLAN.md Phase 2), so we insert
        // directly via the open helper to satisfy the clips table's foreign key for this test.
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO sections (id, projectId, name, orderIndex) VALUES ('$id', 'proj1', 'Verse 1', 0)"
        )
    }

    @Test
    fun insertAndGetClipsForSection_returnsInStartFrameOrder() = runBlocking {
        seedSection()
        db.dao().insertClip(
            ClipEntity(id = "c1", sectionId = "verse1", lane = "Kick", takeId = "take1", startFrame = 44_100L, trimEndFrame = null)
        )
        db.dao().insertClip(
            ClipEntity(id = "c2", sectionId = "verse1", lane = "Snare", takeId = "take1", startFrame = 0L, trimEndFrame = 22_050L)
        )

        val clips = db.dao().getClipsForSection("verse1")
        assertEquals(2, clips.size)
        assertEquals("c2", clips.first().id)
        assertEquals("c1", clips.last().id)
    }

    @Test
    fun deletingSection_cascadesToItsClips() = runBlocking {
        seedSection()
        db.dao().insertClip(
            ClipEntity(id = "c1", sectionId = "verse1", lane = "Kick", takeId = "take1", startFrame = 0L, trimEndFrame = null)
        )
        assertEquals(1, db.dao().getClipsForSection("verse1").size)

        db.openHelper.writableDatabase.execSQL("DELETE FROM sections WHERE id = 'verse1'")

        assertEquals(0, db.dao().getClipsForSection("verse1").size)
        assertNull(db.dao().getClipById("c1"))
    }

    @Test
    fun deleteClip_removesOnlyThatClip() = runBlocking {
        seedSection()
        db.dao().insertClip(
            ClipEntity(id = "c1", sectionId = "verse1", lane = "Kick", takeId = "take1", startFrame = 0L, trimEndFrame = null)
        )
        db.dao().insertClip(
            ClipEntity(id = "c2", sectionId = "verse1", lane = "Snare", takeId = "take1", startFrame = 100L, trimEndFrame = null)
        )

        db.dao().deleteClip("c1")

        val remaining = db.dao().getClipsForSection("verse1")
        assertEquals(1, remaining.size)
        assertEquals("c2", remaining.first().id)
    }
}
