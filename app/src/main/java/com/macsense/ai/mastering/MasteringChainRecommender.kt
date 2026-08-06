package com.macsense.ai.mastering

object MasteringChainRecommender {

    data class MasteringChainProposal(
        val eqLowGain: Float,
        val eqMidGain: Float,
        val eqHighGain: Float,
        val limiterThreshold: Float,
        val compressorThreshold: Float,
        val explanation: String
    )

    fun recommendChain(
        analysis: MasteringAnalyzer.AnalysisResult,
        target: TargetProfile
    ): MasteringChainProposal {
        var eqLow = 0.0f
        var eqMid = 0.0f
        var eqHigh = 0.0f

        val explanations = mutableListOf<String>()

        if (analysis.lowMidRatio > target.targetLowMidRatio * 1.3 && analysis.lowMidRatio > 0.0) {
            eqLow = -1.5f
            eqMid = -2.0f
            explanations.add("Attenuated low-mids (EQ Mid -2.0dB, EQ Low -1.5dB) to clear up the muddy 200-500Hz region.")
        } else if (analysis.lowMidRatio < target.targetLowMidRatio * 0.7 && analysis.lowMidRatio > 0.0) {
            eqLow = 1.0f
            eqMid = 1.5f
            explanations.add("Boosted low-mids (EQ Mid +1.5dB, EQ Low +1.0dB) to add warmth and body to the mix.")
        }

        if (analysis.highRatio > target.targetHighRatio * 1.3 && analysis.highRatio > 0.0) {
            eqHigh = -2.0f
            explanations.add("Tamed harsh high-shelf (EQ High -2.0dB) to reduce sibilance and high-end glare.")
        } else if (analysis.highRatio < target.targetHighRatio * 0.7 && analysis.highRatio > 0.0) {
            eqHigh = 2.0f
            explanations.add("Enhanced high-shelf (EQ High +2.0dB) to inject air, clarity and modern sparkle.")
        }

        val gainNeeded = target.targetLufs - analysis.integratedLufs
        val recommendedLimiterThreshold = if (gainNeeded > 0.0) {
            val th = -gainNeeded.toFloat()
            explanations.add("Set limiter threshold to ${String.format("%.1f", th)}dB to achieve the target loudness of ${target.targetLufs} LUFS.")
            th.coerceIn(-24.0f, 0.0f)
        } else {
            explanations.add("Set limiter threshold to -1.0dB for safe peak ceiling matching.")
            -1.0f
        }

        val recommendedCompressorThreshold = if (analysis.peakDbtp - analysis.integratedLufs > target.dynamicRange) {
            explanations.add("Set compressor threshold to -16.0dB to control excessive transient peaks and increase density.")
            -16.0f
        } else {
            explanations.add("Set compressor threshold to -12.0dB for gentle cohesion ('glue').")
            -12.0f
        }

        val fullExplanation = "Ari's Mastering Proposal for ${target.name}:\n" + explanations.joinToString("\n")

        return MasteringChainProposal(
            eqLowGain = eqLow,
            eqMidGain = eqMid,
            eqHighGain = eqHigh,
            limiterThreshold = recommendedLimiterThreshold,
            compressorThreshold = recommendedCompressorThreshold,
            explanation = fullExplanation
        )
    }
}
