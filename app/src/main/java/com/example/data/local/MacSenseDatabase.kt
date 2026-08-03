package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SoundGenomeEntity::class,
        ProjectEntity::class,
        TrackEntity::class,
        LyricEntity::class,
        VersionNodeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MacSenseDatabase : RoomDatabase() {
    abstract fun dao(): MacSenseDao

    companion object {
        @Volatile
        private var INSTANCE: MacSenseDatabase? = null

        fun getDatabase(context: Context): MacSenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MacSenseDatabase::class.java,
                    "macsense_master_codex.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
