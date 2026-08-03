package com.macsense.ai.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.macsense.ai.data.local.MacSenseDatabase
import com.macsense.ai.data.local.ProjectEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
}
