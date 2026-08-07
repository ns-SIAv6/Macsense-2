package com.macsense.ai.export

import com.macsense.ai.audio.SoundGenome
import kotlin.math.sin
import kotlin.math.abs

object LyricVideoRenderer {
    const val COLOR_VOID_BLACK = 0xFF0A0511.toInt()
    const val COLOR_PANEL_PURPLE = 0xFF150B24.toInt()
    const val COLOR_ACCENT_PURPLE = 0xFF8B5CF6.toInt()
    const val COLOR_GOLD = 0xFFE8B34D.toInt()
    const val COLOR_TEXT_PRIMARY = 0xFFF5F1FA.toInt()
    const val COLOR_TEXT_SECONDARY = 0xFFB3A6C9.toInt()
    const val VIDEO_WIDTH = 1080
    const val VIDEO_HEIGHT = 1920
    const val FRAMES_PER_SECOND = 30

    data class LyricVideoFrame(
        val width: Int, val height: Int, val backgroundColor: Int,
        val waveformAmplitudes: List<Float>, val waveformColor: Int,
        val currentLyricLine: String?, val nextLyricLine: String?,
        val lyricProgress: Float, val accentColor: Int, val brandText: String
    )

    data class TimedLyric(val text: String, val startTimeSeconds: Double, val endTimeSeconds: Double)

    fun parseLrc(lrcContent: String): List<TimedLyric> {
        val result = mutableListOf<TimedLyric>()
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2})\]\s*(.*)""")
        for (line in lrcContent.lines()) {
            val match = regex.matchEntire(line.trim()) ?: continue
            val (min, sec, centi, text) = match.destructured
            val time = min.toInt() * 60 + sec.toInt() + centi.toInt() / 100.0
            result.add(TimedLyric(text.trim(), time, 0.0))
        }
        for (i in result.indices) {
            if (i < result.size - 1) result[i] = result[i].copy(endTimeSeconds = result[i + 1].startTimeSeconds)
            else result[i] = result[i].copy(endTimeSeconds = result[i].startTimeSeconds + 5.0)
        }
        return result
    }

    fun generateFrames(lyrics: List<TimedLyric>, audioAmplitudes: List<Float>, totalDurationSeconds: Double, brandText: String = "MACSENSE"): List<LyricVideoFrame> {
        val totalFrames = (totalDurationSeconds * FRAMES_PER_SECOND).toInt()
        val frames = mutableListOf<LyricVideoFrame>()
        for (frameIndex in 0 until totalFrames) {
            val currentSeconds = frameIndex.toDouble() / FRAMES_PER_SECOND
            val currentLyric = lyrics.find { currentSeconds >= it.startTimeSeconds && currentSeconds < it.endTimeSeconds }
            val nextLyric = lyrics.find { it.startTimeSeconds > currentSeconds }
            val lyricProgress = if (currentLyric != null) {
                val duration = currentLyric.endTimeSeconds - currentLyric.startTimeSeconds
                if (duration > 0) ((currentSeconds - currentLyric.startTimeSeconds) / duration).toFloat().coerceIn(0f, 1f) else 0f
            } else 0f
            val amplitude = if (audioAmplitudes.isNotEmpty()) audioAmplitudes[frameIndex % audioAmplitudes.size] else 0.5f
            val waveformAmplitudes = (0 until 64).map { bar ->
                val phase = bar * 0.15 + frameIndex * 0.08
                val baseAmplitude = amplitude * (0.5 + 0.5 * sin(phase))
                val centerWeight = 1.0 - abs(bar - 32.0) / 32.0
                (baseAmplitude * centerWeight).toFloat().coerceIn(0f, 1f)
            }
            frames.add(LyricVideoFrame(VIDEO_WIDTH, VIDEO_HEIGHT, COLOR_VOID_BLACK, waveformAmplitudes, COLOR_ACCENT_PURPLE, currentLyric?.text, nextLyric?.text, lyricProgress, if (lyricProgress > 0.5f) COLOR_GOLD else COLOR_ACCENT_PURPLE, brandText))
        }
        return frames
    }

    fun computeAmplitudeEnvelope(samples: DoubleArray, sampleRate: Int, totalDurationSeconds: Double): List<Float> {
        val totalFrames = (totalDurationSeconds * FRAMES_PER_SECOND).toInt()
        val samplesPerFrame = (sampleRate / FRAMES_PER_SECOND).toInt()
        return (0 until totalFrames).map { frame ->
            val start = frame * samplesPerFrame
            val end = minOf(start + samplesPerFrame, samples.size)
            if (start >= samples.size) return@map 0f
            var max = 0.0
            for (i in start until end) { val a = abs(samples[i]); if (a > max) max = a }
            max.toFloat().coerceIn(0f, 1f)
        }
    }

    data class LyricVideoSpec(val lyrics: List<TimedLyric>, val audioAmplitudes: List<Float>, val totalDurationSeconds: Double, val brandText: String, val genome: SoundGenome? = null)

    fun createSpec(lrcContent: String, samples: DoubleArray, sampleRate: Int, durationSeconds: Double, genome: SoundGenome? = null): LyricVideoSpec {
        val lyrics = parseLrc(lrcContent)
        val amplitudes = computeAmplitudeEnvelope(samples, sampleRate, durationSeconds)
        return LyricVideoSpec(lyrics, amplitudes, durationSeconds, "MACSENSE", genome)
    }
}
