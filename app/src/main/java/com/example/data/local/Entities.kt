package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sound_genomes")
data class SoundGenomeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val soundType: String,
    val mass: Float,
    val radiance: Float,
    val entropy: Float,
    val curvature: Float,
    val chrom1: Int,
    val chrom2: Int,
    val chrom3: Int,
    val chrom4: Int,
    val chrom5: Int,
    val generation: Int,
    val parentAId: String?,
    val parentBId: String?,
    val scarMagnitude: Float,
    val isExtinct: Boolean,
    val deathTimestamp: Long?,
    val deathReason: String?,
    val epitaph: String?,
    val isFavorite: Boolean
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val genre: String,
    val bpm: Int,
    val keySignature: String,
    val targetLufs: Float,
    val createdTimestamp: Long,
    val modifiedTimestamp: Long
)

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val soundType: String,
    val volume: Float,
    val pan: Float,
    val isMuted: Boolean,
    val isSolo: Boolean,
    val genomeId: String,
    val patternStepsJson: String
)

@Entity(tableName = "lyrics")
data class LyricEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val sectionName: String,
    val lineIndex: Int,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val cadenceScore: Float,
    val rhymeScheme: String
)

@Entity(tableName = "version_nodes")
data class VersionNodeEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val parentId: String?,
    val commitMessage: String,
    val author: String,
    val timestamp: Long,
    val isCurrent: Boolean
)

@Entity(tableName = "breeding_history")
data class BreedingHistoryEntity(
    @PrimaryKey val id: String,
    val parentAId: String,
    val parentAName: String,
    val parentBId: String,
    val parentBName: String,
    val childId: String,
    val childName: String,
    val breedWeight: Float,
    val mutationFactor: Float,
    val generation: Int,
    val timestamp: Long
)

@Entity(tableName = "midi_mappings")
data class MidiMappingEntity(
    @PrimaryKey val id: String,
    val controllerName: String,
    val ccNumber: Int,
    val parameterTarget: String,
    val minValue: Float,
    val maxValue: Float,
    val channel: Int
)

