package com.macsense.ai.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AriCommand(
    val type: String, // "update_bpm", "update_lyrics", "reorder_sections", "apply_preset", "update_effects", "breed_sounds", "resurrect_sound"
    val section_id: String? = null,
    val value: String? = null,
    val preset_name: String? = null,
    val bpm_value: Double? = null,
    val reverb: Float? = null,
    val delay: Float? = null,
    val filter: Float? = null,
    val volume: Float? = null,
    val section_order: List<String>? = null,
    // --- Sound genetics & resurrection (Phase 5) ---
    /** "breed_sounds": id of the first parent take in the archive. */
    val parent_take_id: String? = null,
    /** "breed_sounds": id of the second parent take in the archive. */
    val parent_take_id_2: String? = null,
    /** "breed_sounds": how strongly the offspring leans toward parent_take_id_2's traits, 0.0-1.0. */
    val trait_bias: Double? = null,
    /** "resurrect_sound": id of the DORMANT archive entry to bring back to LIVING/REBORN. */
    val take_id: String? = null,
    /** "breed_sounds" / "resurrect_sound": tags to seed the resulting archive entry with. */
    val tags: List<String>? = null,
    val explanation: String
)

object AriCommandParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): Pair<String, AriCommand?> {
        val startTag = "<ari_command>"
        val endTag = "</ari_command>"
        
        val startIndex = text.indexOf(startTag)
        val endIndex = text.indexOf(endTag)
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            val jsonContent = text.substring(startIndex + startTag.length, endIndex).trim()
            val cleanText = text.replaceRange(startIndex, endIndex + endTag.length, "").trim()
            return try {
                val command = json.decodeFromString<AriCommand>(jsonContent)
                Pair(cleanText, command)
            } catch (e: Exception) {
                Pair(text, null)
            }
        }
        return Pair(text, null)
    }
}
