package com.macsense.ai.export

import kotlinx.serialization.Serializable

@Serializable
enum class ExportFormat(val displayName: String, val fileExtension: String, val description: String) {
    FULL_MIX("Full Mix", "wav", "Complete stereo mix at 24-bit/48kHz"),
    INSTRUMENTAL("Instrumental", "wav", "Full mix with vocal stem muted"),
    ACAPELLA("A Cappella", "wav", "Isolated vocal stem"),
    STEMS_ZIP("Stems ZIP", "zip", "All individual stems as separate WAV files"),
    TIKTOK_15S("TikTok Hook (15s)", "wav", "Most energetic 15 seconds for viral hooks"),
    INSTAGRAM_30S("Instagram Reel (30s)", "wav", "30-second preview optimized for reels"),
    SLOWED_REVERB("Slowed + Reverb", "wav", "Tempo reduced 15%, large reverb, ambient character"),
    SPED_UP("Sped Up", "wav", "Tempo increased 25% by resampling (pitch changes)"),
    AAC_320("AAC 320kbps", "aac", "Compressed AAC at 320kbps for streaming"),
    MP3_320("MP3 320kbps", "mp3", "Compressed MP3 at 320kbps for compatibility");
}

@Serializable
data class TimeConstraint(val startSeconds: Double, val durationSeconds: Double)

@Serializable
data class TempoModulation(val speedFactor: Double, val preservePitch: Boolean, val reverbAmount: Double = 0.0)
