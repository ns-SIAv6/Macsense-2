package com.macsense.ai.audio

/** Measurable, provenance-aware identity for evolving a take without losing its ancestry. */
data class SoundGenome(
    val sourceId: String,
    val transient: Double,
    val harmonicity: Double,
    val brightness: Double,
    val dynamics: Double,
    val stereoWidth: Double = 0.0,
    val confidence: Double = 1.0,
    val parents: List<String> = emptyList()
) {
    init { listOf(transient, harmonicity, brightness, dynamics, stereoWidth, confidence).forEach { require(it in 0.0..1.0) } }

    fun breed(other: SoundGenome, traitsFromOther: Set<Trait>): SoundGenome = copy(
        sourceId = "$sourceId×${other.sourceId}",
        transient = if (Trait.TRANSIENT in traitsFromOther) other.transient else transient,
        harmonicity = if (Trait.HARMONICITY in traitsFromOther) other.harmonicity else harmonicity,
        brightness = if (Trait.BRIGHTNESS in traitsFromOther) other.brightness else brightness,
        dynamics = if (Trait.DYNAMICS in traitsFromOther) other.dynamics else dynamics,
        stereoWidth = if (Trait.STEREO_WIDTH in traitsFromOther) other.stereoWidth else stereoWidth,
        confidence = minOf(confidence, other.confidence),
        parents = listOf(sourceId, other.sourceId)
    )

    fun distanceFrom(other: SoundGenome): Double = listOf(transient - other.transient, harmonicity - other.harmonicity, brightness - other.brightness, dynamics - other.dynamics, stereoWidth - other.stereoWidth).map { it * it }.average().let(::kotlin.math.sqrt)
    enum class Trait { TRANSIENT, HARMONICITY, BRIGHTNESS, DYNAMICS, STEREO_WIDTH }
}
