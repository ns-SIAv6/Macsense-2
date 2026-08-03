package com.macsense.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.Index

@Entity(
    tableName = "projects",
    indices = [Index(value = ["name"])]
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "bpm", defaultValue = "120.0") val bpm: Double
)

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val orderIndex: Int
)

@Entity(tableName = "sound_genomes")
data class SoundGenomeEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val data: String
)

@Entity(tableName = "version_nodes")
data class VersionNodeEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val parentId: String?,
    val timestamp: Long
)
