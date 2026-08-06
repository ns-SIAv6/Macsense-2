package com.macsense.ai.mastering

import com.macsense.ai.dsp.LoudnessMeter
import com.macsense.ai.dsp.SpectrumAnalyzer
import com.macsense.ai.dsp.TruePeakMeter

object MasteringAnalyzer {

    data class AnalysisResult(
        val integratedLufs: Double,
        val peakDbtp: Double,
        val lowMidRatio: Double,
        val highRatio: Double,
        val detectedIssues: List<MasteringIssue>
    )

    data class MasteringIssue(
        val name: String,
        val description: String,
        val severity: Severity
    )

    enum class Severity {
        INFO, WARNING, CRITICAL
    }

    fun analyzeMix(
        channels: Array<DoubleArray>,
        sampleRate: Int,
        target: TargetProfile
    ): AnalysisResult {
        val lufs = LoudnessMeter.integratedLufs(channels, sampleRate)
        val cleanLufs = if (lufs.isInfinite() || lufs.isNaN()) -60.0 else lufs

        val dbtp = TruePeakMeter.measureDbtp(channels.firstOrNull() ?: DoubleArray(0))
        val cleanDbtp = if (dbtp.isInfinite() || dbtp.isNaN()) -60.0 else dbtp

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

        val lowMidRatio = if (totalSum > 0.0) lowMidSum / totalSum else 0.0
        val highRatio = if (totalSum > 0.0) highSum / totalSum else 0.0

        val issues = mutableListOf<MasteringIssue>()

        if (cleanLufs < target.targetLufs - 1.5) {
            val diff = target.targetLufs - cleanLufs
            issues.add(
                MasteringIssue(
                    name = "Insufficient Loudness",
                    description = "The mix integrated loudness is ${String.format("%.1f", cleanLufs)} LUFS, which is ${String.format("%.1f", diff)} dB below the target of ${target.targetLufs} LUFS.",
                    severity = Severity.WARNING
                )
            )
        }

        if (cleanDbtp > target.ceilingDbtp) {
            issues.add(
                MasteringIssue(
                    name = "Clipping Risk",
                    description = "True peak is ${String.format("%.1f", cleanDbtp)} dBTP, which exceeds the target ceiling of ${target.ceilingDbtp} dBTP.",
                    severity = Severity.CRITICAL
                )
            )
        }

        if (lowMidRatio > target.targetLowMidRatio * 1.3 && lowMidRatio > 0.0) {
            issues.add(
                MasteringIssue(
                    name = "Muddy Low-Mids",
                    description = "Excessive energy detected in the 200Hz - 500Hz region. This can make the mix sound cluttered or muddy.",
                    severity = Severity.WARNING
                )
            )
        }

        if (highRatio > target.targetHighRatio * 1.3 && highRatio > 0.0) {
            issues.add(
                MasteringIssue(
                    name = "Harsh High-Shelf",
                    description = "Aggressive high-frequency energy found between 4kHz - 12kHz, indicating potential harshness or sibilance.",
                    severity = Severity.WARNING
                )
            )
        }

        return AnalysisResult(
            integratedLufs = cleanLufs,
            peakDbtp = cleanDbtp,
            lowMidRatio = lowMidRatio,
            highRatio = highRatio,
            detectedIssues = issues
        )
    }
}
