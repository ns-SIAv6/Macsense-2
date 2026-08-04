package com.macsense.ai.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.macsense.ai.data.local.MacSenseDatabase
import com.macsense.ai.data.local.Migrations
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MacSenseDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1)
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migrations.MIGRATION_1_2)
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3() {
        var db = helper.createDatabase(TEST_DB, 2)
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, Migrations.MIGRATION_2_3)
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To3_fullChain() {
        var db = helper.createDatabase(TEST_DB, 1)
        db.close()

        db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3
        )
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3_preservesExistingProjectRows() {
        var db = helper.createDatabase(TEST_DB, 2)
        db.execSQL(
            "INSERT INTO projects (id, name, created_at, updated_at, bpm) VALUES ('p1', 'Pre-migration project', 1000, 2000, 128.0)"
        )
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, Migrations.MIGRATION_2_3)

        val cursor = db.query("SELECT name, bpm FROM projects WHERE id = 'p1'")
        cursor.use {
            assert(it.moveToFirst()) { "Expected pre-migration project row to survive migration to v3" }
            assert(it.getString(0) == "Pre-migration project")
            assert(it.getDouble(1) == 128.0)
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate2To3_createsEmptySoundArchiveTable() {
        var db = helper.createDatabase(TEST_DB, 2)
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, Migrations.MIGRATION_2_3)

        val cursor = db.query("SELECT COUNT(*) FROM sound_archive_entries")
        cursor.use {
            it.moveToFirst()
            assert(it.getInt(0) == 0) { "Expected fresh sound_archive_entries table to be empty right after migration" }
        }
    }
}
