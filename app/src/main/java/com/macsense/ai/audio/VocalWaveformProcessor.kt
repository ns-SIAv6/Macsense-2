package com.macsense.ai.audio

import kotlin.math.*

/**
 * Phase 2 / Milestone 2: Vocal waveform processing for the DAW UI layer.
 *
 * Converts raw PCM samples (ShortArray or FloatArray) into a displayable amplitude
 * envelope and produces forced-alignment time markers for word-level lyric sync.
 *
 * All processing is CPU-only and runs on a background dispatcher — do not call
 * from the main thread. Results are pure data; rendering is in WaveformView.kt.
 */
object VocalWaveformProcessor {

    /**
     * Downsamples a PCM buffer into [barCount] normalized amplitude values [0..1].
     * Suitable for direct use in [WaveformView.amplitudes].
     *
     * @param pcmShorts  Raw signed-16-bit PCM samples at any sample rate.
     * @param barCount   Number of bars in the target display (default 128).
     * @return           List of [barCount] floats in [0..1].
     */
    fun extractEnvelope(pcmShorts: ShortArray, barCount: Int = 128): List<Float> {
        if (pcmShorts.isEmpty()) return List(barCount) { 0f }
        val chunkSize = (pcmShorts.size.toFloat() / barCount).coerceAtLeast(1f).toInt()
        return buildList(barCount) {
            for (b in 0 until barCount) {
                val start = b * chunkSize
                val end = minOf(start + chunkSize, pcmShorts.size)
                if (start >= pcmShorts.size) { add(0f); continue }
                var sumSq = 0.0
                for (i in start until end) {
                    val s = pcmShorts[i].toFloat() / Short.MAX_VALUE
                    sumSq += s * s
                }
                val rms = sqrt(sumSq / (end - start)).toFloat()
                add(rms.coerceIn(0f, 1f))
            }
        }
    }

    /**
     * Float PCM variant (samples in [-1..1]).
     */
    fun extractEnvelopeFloat(pcmFloats: FloatArray, barCount: Int = 128): List<Float> {
        if (pcmFloats.isEmpty()) return List(barCount) { 0f }
        val chunkSize = (pcmFloats.size.toFloat() / barCount).coerceAtLeast(1f).toInt()
        return buildList(barCount) {
            for (b in 0 until barCount) {
                val start = b * chunkSize
                val end = minOf(start + chunkSize, pcmFloats.size)
                if (start >= pcmFloats.size) { add(0f); continue }
                var sumSq = 0.0
                for (i in start until end) sumSq += pcmFloats[i] * pcmFloats[i]
                add(sqrt(sumSq / (end - start)).toFloat().coerceIn(0f, 1f))
            }
        }
    }

    /**
     * Onset detection: returns a list of sample indices where energy onset events occur.
     * Uses a simple spectral flux heuristic (difference in successive RMS chunks).
     *
     * @param pcmFloats      Raw PCM in [-1..1].
     * @param sampleRate     Recording sample rate in Hz (default 44100).
     * @param hopSamples     Number of samples per analysis hop (default 512).
     * @param threshold      Energy-delta threshold for onset detection (default 0.07).
     */
    fun detectOnsets(
        pcmFloats: FloatArray,
        sampleRate: Int = 44100,
        hopSamples: Int = 512,
        threshold: Float = 0.07f
    ): List<Int> {
        if (pcmFloats.size < hopSamples * 2) return emptyList()
        val onsets = mutableListOf<Int>()
        var prevRms = 0f
        var i = 0
        while (i + hopSamples <= pcmFloats.size) {
            var sumSq = 0.0
            for (j in i until i + hopSamples) sumSq += pcmFloats[j] * pcmFloats[j]
            val rms = sqrt(sumSq / hopSamples).toFloat()
            val delta = rms - prevRms
            if (delta > threshold && (onsets.isEmpty() || i - onsets.last() > sampleRate / 20)) {
                onsets += i
            }
            prevRms = rms
            i += hopSamples
        }
        return onsets
    }

    /**
     * Word-level forced alignment (lightweight heuristic).
     * Splits [lyricLine] on whitespace and maps each word to a [0..1] fraction span
     * based on evenly distributing [onsetSamples] across the detected onsets.
     * Returns a list of Pair<startFraction, endFraction> per word.
     *
     * For production accuracy, replace the body with a real CTCDecoder or call a
     * server-side forced-aligner (e.g. Gentle, WhisperX) and pass results in.
     */
    fun alignWordsToOnsets(
        lyricLine: String,
        onsetSamples: List<Int>,
        totalSamples: Int
    ): List<Pair<Float, Float>> {
        val words = lyricLine.trim().split(Regex("\\s+"))
        if (words.isEmpty() || totalSamples == 0) return emptyList()
        if (onsetSamples.isEmpty()) {
            // Fallback: even distribution across the full duration
            return words.mapIndexed { i, _ ->
                val s = i.toFloat() / words.size
                val e = (i + 1).toFloat() / words.size
                s to e
            }
        }
        val anchorCount = minOf(words.size, onsetSamples.size)
        return words.mapIndexed { i, _ ->
            val onsetIdx = (i.toFloat() / words.size * anchorCount).toInt().coerceIn(0, anchorCount - 1)
            val startSample = onsetSamples[onsetIdx]
            val endSample = if (onsetIdx + 1 < onsetSamples.size) onsetSamples[onsetIdx + 1] else totalSamples
            (startSample.toFloat() / totalSamples) to (endSample.toFloat() / totalSamples)
        }
    }
}
