package com.macsense.ai.audio

import com.macsense.ai.dsp.DynamicsAnalyzer
import com.macsense.ai.dsp.OnsetDetector
import com.macsense.ai.dsp.PitchDetector
import com.macsense.ai.dsp.SpectrumAnalyzer
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max

/**
 * Extracts a measurable [SoundGenome] from real PCM using the repo's existing DSP analyzers.
 *
 * Trait mapping:
 * - transient: onset density + average attack sharpness (more/faster attacks -> closer to 1)
 * - harmonicity: pitch detector confidence, damped by spectral flatness/noisiness
 * - brightness: normalized spectral centroid/rolloff blend
 * - dynamics: normalized crest factor / peak-to-average behavior
 * - stereoWidth: currently 0 for mono takes persisted by [AudioCapture]/[PcmFileStore]
 * - confidence: aggregate confidence from pitch confidence + enough signal length/energy
 */
object GenomeExtractor {
    fun extract(sourceId: String, samples: DoubleArray, sampleRate: Int): SoundGenome {
        require(sampleRate > 0) { "sampleRate must be > 0" }

        if (samples.isEmpty()) {
            return SoundGenome(
                sourceId = sourceId,
                transient = 0.0,
                harmonicity = 0.0,
                brightness = 0.0,
                dynamics = 0.0,
                stereoWidth = 0.0,
                confidence = 0.0,
                parents = emptyList()
            )
        }

        val normalized = if (samples.any { it !in -1.0..1.0 }) {
            val peak = samples.maxOf { abs(it) }.takeIf { it > 0.0 } ?: 1.0
            DoubleArray(samples.size) { i -> (samples[i] / peak).coerceIn(-1.0, 1.0) }
        } else samples

        val onsets = OnsetDetector.detectOnsets(normalized, sampleRate)
        val transient = estimateTransient(normalized, sampleRate, onsets)

        val pitch = PitchDetector.detect(normalized, sampleRate)
        val spectrum = SpectrumAnalyzer.analyze(normalized, sampleRate)
        val harmonicity = estimateHarmonicity(pitch?.confidence ?: 0.0, spectrum.flatness)
        val brightness = estimateBrightness(spectrum.centroid, spectrum.rolloff, sampleRate)

        val dynamicsResult = DynamicsAnalyzer.analyze(normalized)
        val dynamics = estimateDynamics(dynamicsResult.crestFactorDb)

        val confidence = estimateConfidence(
            pitchConfidence = pitch?.confidence ?: 0.0,
            sampleCount = normalized.size,
            sampleRate = sampleRate,
            rmsLike = estimateRms(normalized)
        )

        return SoundGenome(
            sourceId = sourceId,
            transient = transient,
            harmonicity = harmonicity,
            brightness = brightness,
            dynamics = dynamics,
            stereoWidth = 0.0,
            confidence = confidence,
            parents = emptyList()
        )
    }

    private fun estimateTransient(samples: DoubleArray, sampleRate: Int, onsets: List<Double>): Double {
        if (samples.isEmpty()) return 0.0
        if (onsets.isEmpty()) return 0.0

        val durationSeconds = max(samples.size.toDouble() / sampleRate, 1e-6)
        val onsetDensity = (onsets.size / durationSeconds / 8.0).coerceIn(0.0, 1.0)

        var attackAccumulator = 0.0
        var counted = 0
        for (onset in onsets) {
            val center = (onset * sampleRate).toInt()
            if (center <= 0 || center >= samples.lastIndex) continue
            val lookahead = minOf(sampleRate / 200, samples.lastIndex - center)
            val lookbehind = minOf(sampleRate / 200, center)
            if (lookahead <= 1 || lookbehind <= 1) continue

            var pre = 0.0
            var post = 0.0
            for (i in center - lookbehind until center) pre += abs(samples[i])
            for (i in center until center + lookahead) post += abs(samples[i])
            pre /= lookbehind
            post /= lookahead
            val sharpness = ((post - pre) / 0.5).coerceIn(0.0, 1.0)
            attackAccumulator += sharpness
            counted++
        }
        val meanAttackSharpness = if (counted > 0) attackAccumulator / counted else 0.0
        return (0.55 * onsetDensity + 0.45 * meanAttackSharpness).coerceIn(0.0, 1.0)
    }

    private fun estimateHarmonicity(pitchConfidence: Double, flatness: Double): Double {
        val tonalness = (1.0 - flatness.coerceIn(0.0, 1.0)).coerceIn(0.0, 1.0)
        return (0.7 * pitchConfidence.coerceIn(0.0, 1.0) + 0.3 * tonalness).coerceIn(0.0, 1.0)
    }

    private fun estimateBrightness(centroidHz: Double, rolloffHz: Double, sampleRate: Int): Double {
        val nyquist = sampleRate / 2.0
        if (nyquist <= 0.0) return 0.0
        val centroidNorm = (centroidHz / nyquist).coerceIn(0.0, 1.0)
        val rolloffNorm = (rolloffHz / nyquist).coerceIn(0.0, 1.0)
        return (0.6 * centroidNorm + 0.4 * rolloffNorm).coerceIn(0.0, 1.0)
    }

    private fun estimateDynamics(crestFactorDb: Double): Double {
        if (!crestFactorDb.isFinite()) return 0.0
        return ((crestFactorDb - 3.0) / 15.0).coerceIn(0.0, 1.0)
    }

    private fun estimateConfidence(pitchConfidence: Double, sampleCount: Int, sampleRate: Int, rmsLike: Double): Double {
        val durationSeconds = sampleCount.toDouble() / sampleRate
        val durationConfidence = (durationSeconds / 0.75).coerceIn(0.0, 1.0)
        val energyConfidence = ((rmsLike - 0.01) / 0.19).coerceIn(0.0, 1.0)
        return (0.4 * pitchConfidence.coerceIn(0.0, 1.0) + 0.35 * durationConfidence + 0.25 * energyConfidence).coerceIn(0.0, 1.0)
    }

    private fun estimateRms(samples: DoubleArray): Double {
        if (samples.isEmpty()) return 0.0
        var sum = 0.0
        for (s in samples) sum += s * s
        return kotlin.math.sqrt(sum / samples.size)
    }
}
