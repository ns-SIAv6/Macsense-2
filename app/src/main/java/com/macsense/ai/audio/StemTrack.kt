package com.macsense.ai.audio

/**
 * Phase 4 (issue #39): stem tracks as first-class objects.
 *
 * Every DAW track is a typed stem with per-stem gain, mute and solo. The 12-lane instrument
 * grid lanes map onto these stem types so existing [com.macsense.ai.data.local.ClipEntity]
 * rows (keyed by lane name) group naturally under a typed stem.
 */
enum class StemType(val displayName: String) {
    VOCALS("Vocals"),
    DRUMS("Drums"),
    BASS("Bass"),
    CHORDS("Chords"),
    FX("FX"),
    ATMOSPHERE("Atmosphere");

    companion object {
        /** Maps an instrument-grid lane name (as used by ClipEntity.lane) to its stem type. */
        fun fromLane(lane: String): StemType = when (lane) {
            "Kick", "Snare", "Hi-Hat", "Clap", "Percussion", "Crash" -> DRUMS
            "808/Bass", "Bass Synth" -> BASS
            "Lead", "Pads" -> CHORDS
            "Vocal/Adlib" -> VOCALS
            "Riser" -> FX
            else -> ATMOSPHERE
        }
    }
}

/** A typed stem track with its own mix state. */
data class StemTrack(
    val id: String,
    val type: StemType,
    val name: String = type.displayName,
    val gainDb: Float = 0f,
    val muted: Boolean = false,
    val soloed: Boolean = false,
)

/**
 * Pure mix-state resolver for a set of stem tracks.
 *
 * Solo semantics match every mainstream DAW: if ANY stem is soloed, only soloed stems are
 * audible (mute still wins over solo on the same stem). Gain is converted from dB to a
 * linear multiplier, clamped to a sane fader range of -60..+12 dB.
 */
object StemMixer {
    const val MIN_GAIN_DB = -60f
    const val MAX_GAIN_DB = 12f

    fun clampGainDb(gainDb: Float): Float = gainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)

    fun linearGain(gainDb: Float): Float {
        val clamped = clampGainDb(gainDb)
        if (clamped <= MIN_GAIN_DB) return 0f
        return Math.pow(10.0, clamped / 20.0).toFloat()
    }

    /** True if this stem should be audible given the whole mixer's solo state. */
    fun isAudible(stem: StemTrack, tracks: List<StemTrack>): Boolean {
        if (stem.muted) return false
        val anySolo = tracks.any { it.soloed }
        return !anySolo || stem.soloed
    }

    /** Effective linear gain per stem id: 0 when inaudible, dB→linear otherwise. */
    fun effectiveGains(tracks: List<StemTrack>): Map<String, Float> =
        tracks.associate { stem ->
            stem.id to if (isAudible(stem, tracks)) linearGain(stem.gainDb) else 0f
        }
}
