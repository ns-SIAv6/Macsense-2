package com.macsense.ai.data.local

import androidx.room.TypeConverter
import com.macsense.ai.audio.SoundArchive
import com.macsense.ai.audio.SoundGenome
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }
    }

    @TypeConverter
    fun fromStringSet(value: Set<String>?): String {
        return value?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toStringSet(value: String?): Set<String> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    @TypeConverter
    fun fromSoundGenome(genome: SoundGenome?): String? {
        return genome?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toSoundGenome(data: String?): SoundGenome? {
        return data?.let { Json.decodeFromString<SoundGenome>(it) }
    }

    @TypeConverter
    fun fromArchiveState(state: SoundArchive.State?): String? {
        return state?.name
    }

    @TypeConverter
    fun toArchiveState(name: String?): SoundArchive.State? {
        return name?.let { SoundArchive.State.valueOf(it) }
    }

    /**
     * Serializes the 12-lane step-sequencer grid (`Map<String, List<Boolean>>`) into a compact
     * JSON object for [SectionEntity.instrumentGridJson], e.g. `{"Kick":[true,false,...]}`.
     */
    @TypeConverter
    fun fromInstrumentGrid(grid: Map<String, List<Boolean>>?): String {
        if (grid == null) return "{}"
        return Json.encodeToString(grid)
    }

    @TypeConverter
    fun toInstrumentGrid(json: String?): Map<String, List<Boolean>> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
