package com.macsense.ai.audio

/**
 * Vocal preset scanner: maps a [ScanMode] to a concrete [VocalPreset] that configures
 * the full plugin chain (AutoTune speed, EQ, compression, spatial FX).
 *
 * Used by VocalScannerScreen and the Ari command surface to apply reference-track
 * analysis results in a structured, type-safe way.
 */
object VocalPresetScanner {

    enum class ScanMode { MATCH_CLOSELY, FIT_MY_VOICE, BLEND_STYLES }

    /**
     * Full plugin chain preset applied after a vocal scan.
     *
     * @param autoTuneSpeedMs   Retune speed in ms (low = robotic, high = natural).
     * @param eqLowDb           Low shelf gain in dB.
     * @param eqMidDb           Mid presence boost/cut in dB.
     * @param eqHighDb          High air shelf in dB.
     * @param compThresholdDb   Compressor threshold in dBFS.
     * @param compRatio         Compressor ratio (e.g. 4.0 = 4:1).
     * @param reverbMixPct      Reverb send level in % [0..100].
     * @param delayFeedbackPct  Delay feedback in % [0..100].
     * @param label             Human-readable preset name.
     */
    data class VocalPreset(
        val autoTuneSpeedMs: Float,
        val eqLowDb: Float,
        val eqMidDb: Float,
        val eqHighDb: Float,
        val compThresholdDb: Float,
        val compRatio: Float,
        val reverbMixPct: Float,
        val delayFeedbackPct: Float,
        val label: String
    )

    /** Base presets per mode. Reference-track analysis may refine these values. */
    private val BASE_PRESETS = mapOf(
        ScanMode.MATCH_CLOSELY to VocalPreset(
            autoTuneSpeedMs = 3f,
            eqLowDb = -4f, eqMidDb = 0.5f, eqHighDb = 6f,
            compThresholdDb = -22f, compRatio = 6f,
            reverbMixPct = 35f, delayFeedbackPct = 45f,
            label = "Match Closely"
        ),
        ScanMode.FIT_MY_VOICE to VocalPreset(
            autoTuneSpeedMs = 18f,
            eqLowDb = -1f, eqMidDb = 2f, eqHighDb = 3.5f,
            compThresholdDb = -14f, compRatio = 3.5f,
            reverbMixPct = 15f, delayFeedbackPct = 20f,
            label = "Fit My Voice"
        ),
        ScanMode.BLEND_STYLES to VocalPreset(
            autoTuneSpeedMs = 10f,
            eqLowDb = -2.5f, eqMidDb = 1f, eqHighDb = 4.5f,
            compThresholdDb = -18f, compRatio = 4.5f,
            reverbMixPct = 25f, delayFeedbackPct = 35f,
            label = "Blend Styles"
        )
    )

    /**
     * Returns a [VocalPreset] for the given mode, optionally refined by reference-track
     * analysis data (perceivedLoudnessDb, estimatedReverbPct, spectralBrightness [0..1]).
     *
     * The refinement shifts each parameter proportionally toward what the reference
     * audio implies, bounded by sensible limits.
     */
    fun computePreset(
        mode: ScanMode,
        perceivedLoudnessDb: Float? = null,
        estimatedReverbPct: Float? = null,
        spectralBrightness: Float? = null
    ): VocalPreset {
        val base = BASE_PRESETS.getValue(mode)
        return base.copy(
            compThresholdDb = perceivedLoudnessDb?.let {
                (base.compThresholdDb + it * 0.3f).coerceIn(-48f, -6f)
            } ?: base.compThresholdDb,
            reverbMixPct = estimatedReverbPct?.let {
                ((base.reverbMixPct + it) / 2f).coerceIn(0f, 80f)
            } ?: base.reverbMixPct,
            eqHighDb = spectralBrightness?.let {
                (base.eqHighDb + it * 4f - 2f).coerceIn(-12f, 12f)
            } ?: base.eqHighDb
        )
    }

    /**
     * Parses a mode string from the UI (as stored in VocalScannerScreen state)
     * into the enum. Defaults to [ScanMode.FIT_MY_VOICE].
     */
    fun modeFromString(s: String): ScanMode = when (s) {
        "Match Closely" -> ScanMode.MATCH_CLOSELY
        "Blend Styles" -> ScanMode.BLEND_STYLES
        else -> ScanMode.FIT_MY_VOICE
    }
}
