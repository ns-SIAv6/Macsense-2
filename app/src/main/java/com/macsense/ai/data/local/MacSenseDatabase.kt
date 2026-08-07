package com.macsense.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProjectEntity::class,
        SectionEntity::class,
        SoundGenomeEntity::class,
        VersionNodeEntity::class,
        SoundArchiveEntryEntity::class,
        ClipEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MacSenseDatabase : RoomDatabase() {
    abstract fun dao(): MacSenseDao
}
