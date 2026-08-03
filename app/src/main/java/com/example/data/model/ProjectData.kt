package com.example.data.model

data class Project(
    val id: String = "project_master_01",
    val title: String = "Cyber Symphony No. 1",
    val genre: String = "Futuristic Trap / Cyberpunk",
    val bpm: Int = 140,
    val keySignature: String = "C Minor",
    val targetLufs: Float = -14.0f,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val modifiedTimestamp: Long = System.currentTimeMillis()
)

data class SectionCard(
    val id: String,
    val name: String, // "Intro", "Verse 1", "Hook", "Verse 2", "Bridge", "Outro"
    val barStart: Int,
    val barLength: Int,
    val energyLevel: Float, // 0.0 to 1.0
    val colorHex: String,
    val isPlaying: Boolean = false
)

data class TrackItem(
    val id: String,
    val name: String,
    val soundType: SoundType,
    val volume: Float = 0.8f,
    val pan: Float = 0.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val genomeId: String,
    val patternSteps: List<Boolean> = List(16) { it % 4 == 0 } // Step sequencer pattern
)

data class LyricSpan(
    val id: String,
    val sectionName: String,
    val lineIndex: Int,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val cadenceScore: Float = 0.85f,
    val rhymeScheme: String = "AABB",
    val isSelected: Boolean = false
)

data class VersionNode(
    val id: String,
    val parentId: String?,
    val commitMessage: String,
    val author: String = "ARi Co-Producer",
    val timestamp: Long = System.currentTimeMillis(),
    val isCurrent: Boolean = false
)

data class CompingTake(
    val id: String,
    val trackId: String,
    val takeNumber: Int,
    val durationMs: Long,
    val ratingStars: Int = 4,
    val selectedSpan: String = "Bar 1-4"
)

data class MarketplaceSample(
    val id: String,
    val name: String,
    val creator: String,
    val soundType: SoundType,
    val priceTokens: Int = 0,
    val mass: Float,
    val radiance: Float,
    val entropy: Float,
    val curvature: Float,
    val downloads: Int
)

data class WhisperChip(
    val id: String,
    val tier: Tier, // NOTIFY, QUESTION, REVIEW
    val sourceThread: String, // Breeder, Archivist, Lyricist, Ear
    val message: String,
    val actionText: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Tier {
        NOTIFY,   // Informational, subtle
        QUESTION, // Interactive choice
        REVIEW    // Critical action required
    }
}

data class MultiAgentMessage(
    val id: String,
    val agentName: String, // "ARi Breeder", "ARi Lyricist", "ARi Engineer"
    val avatarRole: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
