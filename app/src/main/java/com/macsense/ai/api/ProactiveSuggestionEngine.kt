package com.macsense.ai.api

import com.macsense.ai.dsp.DynamicsResult
import com.macsense.ai.dsp.TempoResult
import java.util.Locale

enum class Severity {
    INFO,
    WARNING,
    CRITICAL
}

data class ProactiveSuggestion(
    val severity: Severity,
    val title: String,
    val description: String,
    val suggestedAction: String
)

data class DspAnalysisData(
    val truePeakDbtp: Double? = null,
    val dynamicsResult: DynamicsResult? = null,
    val integratedLufs: Double? = null,
    val tempoResult: TempoResult? = null
)

object ProactiveSuggestionEngine {

    fun analyze(
        truePeakDbtp: Double? = null,
        dynamicsResult: DynamicsResult? = null,
        integratedLufs: Double? = null,
        tempoResult: TempoResult? = null
    ): List<ProactiveSuggestion> {
        return analyze(DspAnalysisData(truePeakDbtp, dynamicsResult, integratedLufs, tempoResult))
    }

    fun analyze(data: DspAnalysisData): List<ProactiveSuggestion> {
        val suggestions = mutableListOf<ProactiveSuggestion>()

        data.truePeakDbtp?.let { peak ->
            if (peak > 0.0) {
                suggestions.add(
                    ProactiveSuggestion(
                        severity = Severity.CRITICAL,
                        title = "True Peak Clipping Detected",
                        description = "True peak level reached ${String.format(Locale.US, "%.1f", peak)} dBTP, causing digital distortion.",
                        suggestedAction = "Reduce master gain or output makeup gain immediately."
                    )
                )
            } else if (peak > -1.0) {
                suggestions.add(
                    ProactiveSuggestion(
                        severity = Severity.WARNING,
                        title = "High True Peak Level",
                        description = "True peak is ${String.format(Locale.US, "%.1f", peak)} dBTP, which exceeds the -1.0 dBTP ceiling for streaming platforms.",
                        suggestedAction = "Reduce makeup gain by at least 1-2 dB."
                    )
                )
            }
        }

        data.dynamicsResult?.let { dyn ->
            if (dyn.crestFactorDb < 6.0) {
                suggestions.add(
                    ProactiveSuggestion(
                        severity = Severity.WARNING,
                        title = "Over-Compressed Dynamics",
                        description = "Dynamic range / crest factor is very low (${String.format(Locale.US, "%.1f", dyn.crestFactorDb)} dB), resulting in a flat sound profile.",
                        suggestedAction = "Ease limiter/compressor settings to add dynamic variation between sections."
                    )
                )
            }
        }

        data.integratedLufs?.let { lufs ->
            if (lufs > -8.0 && lufs.isFinite()) {
                suggestions.add(
                    ProactiveSuggestion(
                        severity = Severity.WARNING,
                        title = "Excessive Loudness",
                        description = "Integrated loudness is ${String.format(Locale.US, "%.1f", lufs)} LUFS, which may suffer heavy normalization penalty on streaming services.",
                        suggestedAction = "Lower overall mix level to target -14 to -10 LUFS."
                    )
                )
            } else if (lufs < -24.0 && lufs.isFinite()) {
                suggestions.add(
                    ProactiveSuggestion(
                        severity = Severity.INFO,
                        title = "Low Integrated Loudness",
                        description = "Integrated loudness is ${String.format(Locale.US, "%.1f", lufs)} LUFS, which is below commercial target levels.",
                        suggestedAction = "Apply gentle master bus compression or gain boost."
                    )
                )
            }
        }

        data.tempoResult?.let { tempo ->
            if (tempo.confidence in 0.001..0.35) {
                suggestions.add(
                    ProactiveSuggestion(
                        severity = Severity.INFO,
                        title = "Uncertain Tempo Estimation",
                        description = "Detected ${String.format(Locale.US, "%.0f", tempo.bpm)} BPM with low confidence (${String.format(Locale.US, "%.0f", tempo.confidence * 100)}%).",
                        suggestedAction = "Use tap tempo or align grid markers manually."
                    )
                )
            }
        }

        return suggestions
    }
}
