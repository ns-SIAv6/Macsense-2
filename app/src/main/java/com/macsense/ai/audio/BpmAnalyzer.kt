package com.macsense.ai.audio

import kotlin.math.*

/**
 * Beat/tempo analysis utilities for FlowCaptureScreen's BPM detection and
 * onset-based elastic time-alignment ("Natural" vs "Manual BPM" modes).
 *
 * All functions are pure (no side effects) and designed to run on IO/Default dispatcher.
 */
object BpmAnalyzer {

    /**
     * Estimates BPM from a list of onset sample indices.
     *
     * Algorithm: compute all inter-onset intervals (IOIs) in milliseconds,
     * then find the most common tempo via a histogram bucketed in 1 BPM increments.
     *
     * @param onsetSamples  Sample indices of detected onsets (from VocalWaveformProcessor).
     * @param sampleRate    Recording sample rate in Hz.
     * @return              Estimated BPM in [60..220], or 0 if estimation failed.
     */
    fun estimateBpm(onsetSamples: List<Int>, sampleRate: Int = 44100): Double {
        if (onsetSamples.size < 4) return 0.0

        val ioisMs = onsetSamples.zipWithNext { a, b ->
            (b - a).toDouble() / sampleRate * 1000.0
        }.filter { it in 136.0..2000.0 } // 30–440 BPM window

        if (ioisMs.isEmpty()) return 0.0

        // Convert each IOI to a candidate BPM and find modal cluster
        val bpmCandidates = ioisMs.map { 60000.0 / it }
        val histogram = mutableMapOf<Int, Int>()
        bpmCandidates.forEach { bpm ->
            val bucket = bpm.roundToInt().coerceIn(60, 220)
            histogram[bucket] = (histogram[bucket] ?: 0) + 1
        }

        val peakBucket = histogram.maxByOrNull { it.value }?.key ?: return 0.0
        // Refine with median of candidates close to peak bucket
        val refined = bpmCandidates.filter { abs(it - peakBucket) < 5.0 }
        return if (refined.isEmpty()) peakBucket.toDouble() else refined.median()
    }

    /**
     * Elastic time-alignment: maps onset frames to a uniform "quantized" grid.
     *
     * Returns a new list of frame indices warped to the nearest beat position
     * at [targetBpm], preserving relative ordering.
     *
     * @param onsetSamples   Detected onset sample indices.
     * @param targetBpm      The target BPM grid to align to.
     * @param sampleRate     Recording sample rate in Hz.
     * @param swing          Swing factor [0..0.5] — 0 = straight, 0.5 = hard swing.
     */
    fun elasticAlignToGrid(
        onsetSamples: List<Int>,
        targetBpm: Double,
        sampleRate: Int = 44100,
        swing: Float = 0f
    ): List<Int> {
        if (onsetSamples.isEmpty() || targetBpm <= 0) return onsetSamples
        val beatSamples = (60.0 / targetBpm * sampleRate).toFloat()
        val subdivSamples = beatSamples / 2f // 8ths

        return onsetSamples.map { onset ->
            val nearestSubdiv = (onset / subdivSamples).roundToInt()
            // Apply swing: even subdivisions are pushed slightly forward
            val swingOffset = if (nearestSubdiv % 2 == 1) (subdivSamples * swing).toInt() else 0
            (nearestSubdiv * subdivSamples).toInt() + swingOffset
        }
    }

    /**
     * Computes the "flow confidence" score: how tightly [onsetSamples] cluster to
     * the nearest beat grid at [bpm]. Returns a value in [0..1] (1 = perfect grid).
     */
    fun flowConfidence(onsetSamples: List<Int>, bpm: Double, sampleRate: Int = 44100): Float {
        if (onsetSamples.isEmpty() || bpm <= 0) return 0f
        val beatSamples = 60.0 / bpm * sampleRate
        val deviations = onsetSamples.map { onset ->
            val nearestBeat = (onset / beatSamples).roundToInt() * beatSamples
            abs(onset - nearestBeat) / beatSamples
        }
        val meanDev = deviations.average()
        return (1f - meanDev.toFloat() * 2f).coerceIn(0f, 1f)
    }

    private fun List<Double>.median(): Double {
        if (isEmpty()) return 0.0
        val sorted = sorted()
        return if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        } else {
            sorted[sorted.size / 2]
        }
    }

    private fun Double.roundToInt() = kotlin.math.round(this).toInt()
    private fun Float.roundToInt() = kotlin.math.round(this).toInt()
}
