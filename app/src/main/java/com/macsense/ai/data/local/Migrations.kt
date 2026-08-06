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

    /**
     * Adds the `clips` table: the first concrete piece of Phase 2's "extend Room to a full
     * track/clip/region schema" item in PRODUCTION_HARDENING_PLAN.md. See [ClipEntity] kdoc for
     * the schema rationale (lane-as-track, take-id reference instead of duplicated audio,
     * per-clip trim/gain/mute, CASCADE delete tied to the owning section).
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `clips` (" +
                    "`id` TEXT NOT NULL, " +
                    "`section_id` TEXT NOT NULL, " +
                    "`lane` TEXT NOT NULL, " +
                    "`take_id` TEXT NOT NULL, " +
                    "`start_frame` INTEGER NOT NULL, " +
                    "`trim_start_frame` INTEGER NOT NULL DEFAULT 0, " +
                    "`trim_end_frame` INTEGER, " +
                    "`gain_db` REAL NOT NULL DEFAULT 0.0, " +
                    "`muted` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`section_id`) REFERENCES `sections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_clips_section_id` ON `clips` (`section_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_clips_take_id` ON `clips` (`take_id`)")
        }
    }

    /**
     * Adds the `project_snapshots` table backing Phase 2's autosave item: one row per project
     * holding the last-saved bpm + serialized section list, replaced on every autosave rather
     * than accumulating history (transaction-level undo/redo is handled in-memory instead).
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `project_snapshots` (" +
                    "`project_id` TEXT NOT NULL, " +
                    "`bpm` REAL NOT NULL, " +
                    "`sections_json` TEXT NOT NULL, " +
                    "`saved_at` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`project_id`))"
            )
        }
    }
}
