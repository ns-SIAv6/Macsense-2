package com.macsense.ai.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE projects ADD COLUMN bpm REAL NOT NULL DEFAULT 120.0")
            db.execSQL("CREATE TABLE IF NOT EXISTS `sections` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `name` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `sound_genomes` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `data` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `version_nodes` (`id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `parentId` TEXT, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        }
    }
}
