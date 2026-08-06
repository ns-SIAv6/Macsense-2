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
        // Safety net for PRODUCTION_GAP_ANALYSIS.md item A / issue tracked in
        // PRODUCTION_HARDENING_PLAN.md Phase 1: if a future schema bump ships without a matching
        // Migration object (a missed migration), Room would otherwise crash on database open on
        // upgrade. Falling back to destructive recreation trades data loss for app-breaking crash
        // in that specific failure mode, which is the correct tradeoff for a local-only project
        // cache — user projects still live in exportable form (see DEPLOYMENT_GUIDE.md backup
        // guidance) and a crash-on-launch is strictly worse for every user, not just the ones
        // affected by the missed migration.
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()
    }

    val repository: MacSenseRepository by lazy {
        MacSenseRepository(database.dao())
    }
}
