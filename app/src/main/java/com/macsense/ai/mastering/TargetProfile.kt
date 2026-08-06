package com.macsense.ai.mastering

import com.macsense.ai.dsp.LoudnessMeter
import com.macsense.ai.dsp.SpectrumAnalyzer
import com.macsense.ai.dsp.TruePeakMeter
import kotlin.math.max

data class TargetProfile(
    val id: String,
    val name: String,
    val targetLufs: Double,
    val ceilingDbtp: Double,
    val targetLowMidRatio: Double,
    val targetHighRatio: Double,
    val dynamicRange: Double,
    val description: String
) {
    companion object {
        val POP = TargetProfile(
            id = "pop",
            name = "Pop Reference",
            targetLufs = -10.0,
            ceilingDbtp = -1.0,
            targetLowMidRatio = 0.15,
            targetHighRatio = 0.08,
            dynamicRange = 9.0,
            description = "Balanced, bright and competitive pop master."
        )

        val HIP_HOP = TargetProfile(
            id = "hiphop",
            name = "Hip Hop / Trap",
            targetLufs = -9.0,
            ceilingDbtp = -0.5,
            targetLowMidRatio = 0.22,
            targetHighRatio = 0.06,
            dynamicRange = 8.5,
            description = "Warm low-mids, punchy sub-bass, and clear vocals."
        )

        val ELECTRONIC = TargetProfile(
            id = "electronic",
            name = "Electronic Standard",
            targetLufs = -8.0,
            ceilingDbtp = -0.5,
            targetLowMidRatio = 0.12,
            targetHighRatio = 0.10,
            dynamicRange = 7.5,
            description = "Loud, flat spectrum, high energetic treble and deep sub."
        )

        val ACOUSTIC = TargetProfile(
            id = "acoustic",
            name = "Acoustic / Jazz",
            targetLufs = -14.0,
            ceilingDbtp = -1.0,
            targetLowMidRatio = 0.18,
            targetHighRatio = 0.05,
            dynamicRange = 12.0,
            description = "Wide dynamic range, natural acoustics, high fidelity."
        )

        val DEFAULT_PROFILES = listOf(POP, HIP_HOP, ELECTRONIC, ACOUSTIC)

        fun importFromReferenceTrack(
            channels: Array<DoubleArray>,
            sampleRate: Int,
            name: String
        ): TargetProfile {
            val lufs = LoudnessMeter.integratedLufs(channels, sampleRate)
            val cleanLufs = if (lufs.isInfinite() || lufs.isNaN()) -14.0 else lufs.coerceIn(-24.0, -6.0)

            val dbtp = TruePeakMeter.measureDbtp(channels.firstOrNull() ?: DoubleArray(0))
            val cleanDbtp = if (dbtp.isInfinite() || dbtp.isNaN()) -1.0 else dbtp.coerceIn(-6.0, 0.0)

            val mixLeft = channels.firstOrNull() ?: DoubleArray(0)
            val spec = SpectrumAnalyzer.analyze(mixLeft, sampleRate)

            val mag = spec.magnitudes
            var totalSum = 0.0
            var lowMidSum = 0.0
            var highSum = 0.0
            val n = mag.size
            for (i in 0 until n) {
                val freq = i * sampleRate.toDouble() / (n * 2)
                totalSum += mag[i]
                if (freq in 200.0..500.0) {
                    lowMidSum += mag[i]
                } else if (freq in 4000.0..12000.0) {
                    highSum += mag[i]
                }
            }

            val lowMidRatio = if (totalSum > 0.0) lowMidSum / totalSum else 0.15
            val highRatio = if (totalSum > 0.0) highSum / totalSum else 0.08
            val dynRange = max(1.0, cleanDbtp - cleanLufs)

            return TargetProfile(
                id = "imported_" + System.currentTimeMillis(),
                name = name,
                targetLufs = cleanLufs,
                ceilingDbtp = cleanDbtp,
                targetLowMidRatio = lowMidRatio,
                targetHighRatio = highRatio,
                dynamicRange = dynRange,
                description = "Imported reference profile."
            )
        }
    }
}
