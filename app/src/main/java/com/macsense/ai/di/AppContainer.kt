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
        .build()
    }

    val repository: MacSenseRepository by lazy {
        MacSenseRepository(database.dao())
    }
}
