package com.macsense.ai.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AriCommand(
    val type: String, // "update_bpm", "update_lyrics", "reorder_sections", "apply_preset", "update_effects"
    val section_id: String? = null,
    val value: String? = null,
    val preset_name: String? = null,
    val bpm_value: Double? = null,
    val reverb: Float? = null,
    val delay: Float? = null,
    val filter: Float? = null,
    val volume: Float? = null,
    val section_order: List<String>? = null,
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
