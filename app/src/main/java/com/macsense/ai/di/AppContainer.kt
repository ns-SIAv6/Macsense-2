package com.macsense.ai.di

import android.content.Context
import androidx.room.Room
import com.macsense.ai.data.local.MacSenseDatabase
import com.macsense.ai.data.local.Migrations
import com.macsense.ai.data.repository.MacSenseRepository

class AppContainer(private val context: Context) {
    val database: MacSenseDatabase by lazy {
        val builder = Room.databaseBuilder(
            context,
            MacSenseDatabase::class.java,
            "macsense_db"
        )
        .addMigrations(Migrations.MIGRATION_1_2, Migrations.MIGRATION_2_3, Migrations.MIGRATION_3_4, Migrations.MIGRATION_4_5, Migrations.MIGRATION_5_6)
        // Never delete musical projects or genome data merely because a schema cannot
        // migrate. Room fails loudly, preserving the database for a supported upgrade
        // or recovery path instead of silently constructing an empty replacement.
        builder.build()
    }

    val repository: MacSenseRepository by lazy {
        MacSenseRepository(database.dao())
    }
}
