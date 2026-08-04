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

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `sound_archive_entries` (" +
                    "`takeId` TEXT NOT NULL, " +
                    "`state` TEXT NOT NULL, " +
                    "`tags` TEXT NOT NULL, " +
                    "`genome_data` TEXT, " +
                    "`origin_take_id` TEXT, " +
                    "PRIMARY KEY(`takeId`))"
            )
        }
    }
}
