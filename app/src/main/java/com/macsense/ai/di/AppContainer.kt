package com.macsense.ai.di

import android.content.Context
import androidx.room.Room
import com.macsense.ai.data.local.MacSenseDatabase
import com.macsense.ai.data.local.Migrations
import com.macsense.ai.data.repository.MacSenseRepository

class AppContainer(private val context: Context) {
    val database: MacSenseDatabase by lazy {
        Room.databaseBuilder(
            context,
            MacSenseDatabase::class.java,
            "macsense_db"
        )
        .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3)
        // Safety net for PRODUCTION_GAP_ANALYSIS.md item A ("Room migration strategy ... has no
        // destructive-migration fallback declared, so a missed migration in a future release
        // would crash on upgrade") and PRODUCTION_HARDENING_PLAN.md Phase 1.
        //
        // fallbackToDestructiveMigration() covers the upgrade case (schema version bumped without
        // a matching Migration object being added/wired above — exactly the scenario the gap
        // analysis warns about). fallbackToDestructiveMigrationOnDowngrade() covers the separate
        // downgrade case (e.g. a user sideloads an older build after a newer one already migrated
        // the schema forward).
        //
        // Trading data loss for a hard crash-on-launch is the correct tradeoff here: this is a
        // local-only project cache (see DEPLOYMENT_GUIDE.md), and a full app crash on launch is
        // strictly worse for every affected user than losing locally cached project data on the
        // rare missed-migration path. A future improvement (tracked in issue #52) is exporting
        // an on-disk backup of the database file before a destructive recreation runs.
        .fallbackToDestructiveMigration()
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()
    }

    val repository: MacSenseRepository by lazy {
        MacSenseRepository(database.dao())
    }
}
