package com.macsense.ai.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression test for PRODUCTION_GAP_ANALYSIS.md item A: "Room migration strategy exists
 * (MIGRATION_1_2) but has no destructive-migration fallback declared, so a missed migration in a
 * future release would crash on upgrade."
 *
 * This opens a database file at the CURRENT schema version, then reopens the *same file* against
 * a database class declaring a higher version with NO migration registered for that jump —
 * simulating exactly the "missed migration" scenario the gap analysis warns about. Without
 * `fallbackToDestructiveMigration()`, Room throws `IllegalStateException` here. With it wired (as
 * in [com.macsense.ai.di.AppContainer]), the database recreates its schema instead of crashing.
 */
@RunWith(RobolectricTestRunner::class)
class DestructiveMigrationFallbackTest {

    @Database(entities = [ProjectEntity::class], version = 99, exportSchema = false)
    @TypeConverters(Converters::class)
    abstract class FutureVersionDatabaseNoFallback : androidx.room.RoomDatabase() {
        abstract fun dao(): MacSenseDao
    }

    private val dbName = "fallback_test_${System.nanoTime()}.db"

    @Test(expected = IllegalStateException::class)
    fun reopeningAtHigherVersionWithoutFallback_throws() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Open + close at the real, current schema version first.
        val original = Room.databaseBuilder(context, MacSenseDatabase::class.java, dbName)
            .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
            .build()
        original.openHelper.writableDatabase
        original.close()

        // Reopen the same file against a higher-version schema with no migration path and no
        // fallback configured — this is the crash this fix prevents in AppContainer.
        val brokenUpgrade = Room.databaseBuilder(context, FutureVersionDatabaseNoFallback::class.java, dbName)
            .build()
        brokenUpgrade.openHelper.writableDatabase
    }

    @Test
    fun reopeningAtHigherVersionWithFallback_recreatesInsteadOfCrashing() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val original = Room.databaseBuilder(context, MacSenseDatabase::class.java, dbName)
            .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
            .build()
        original.openHelper.writableDatabase
        original.close()

        // Same missed-migration scenario as above, but this time with the fallback that
        // AppContainer now configures in production.
        val recovered = Room.databaseBuilder(context, FutureVersionDatabaseNoFallback::class.java, dbName)
            .fallbackToDestructiveMigration()
            .build()

        val db = recovered.openHelper.writableDatabase
        assertEquals(99, db.version)
        recovered.close()
    }
}
