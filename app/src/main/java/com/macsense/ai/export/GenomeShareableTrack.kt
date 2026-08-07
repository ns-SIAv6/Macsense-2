package com.macsense.ai.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.macsense.ai.audio.SoundGenome

@Serializable
data class GenomeShareableTrack(
    val genome: SoundGenome,
    val trackName: String,
    val creatorName: String,
    val macsenseVersion: String = "1.0",
    val exportedAt: Long,
    val tags: List<String> = emptyList(),
    val lineageSummary: String? = null
) {
    companion object {
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
        const val MAGIC = "MACSENSE_DNA_V1"

        fun toShareableJson(track: GenomeShareableTrack): String {
            val payload = json.encodeToString(track)
            return "# $MAGIC\n# Track: ${track.trackName}\n# Creator: ${track.creatorName}\n# Exported: ${track.exportedAt}\n# Breed this sound — import into MacSense and mutate freely.\n\n$payload"
        }

        fun fromShareableJson(raw: String): GenomeShareableTrack {
            val jsonBody = raw.lines().filterNot { it.startsWith("#") }.joinToString("\n").trim()
            return json.decodeFromString(jsonBody)
        }
    }
}
