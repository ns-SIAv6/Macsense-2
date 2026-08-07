package com.macsense.ai.export

import org.junit.Assert.*
import org.junit.Test

class LyricVideoRendererTest {

    @Test
    fun `parseLrc parses standard LRC format`() {
        val lrc = "[00:01.00]First line\n[00:03.50]Second line\n[00:08.00]Third line"
        val lyrics = LyricVideoRenderer.parseLrc(lrc)
        assertEquals(3, lyrics.size)
        assertEquals("First line", lyrics[0].text)
        assertEquals(1.0, lyrics[0].startTimeSeconds, 0.01)
        assertEquals(3.5, lyrics[1].startTimeSeconds, 0.01)
        assertEquals(8.0, lyrics[2].startTimeSeconds, 0.01)
    }

    @Test
    fun `parseLrc fills end times from next line start`() {
        val lrc = "[00:01.00]Line one\n[00:03.00]Line two"
        val lyrics = LyricVideoRenderer.parseLrc(lrc)
        assertEquals(3.0, lyrics[0].endTimeSeconds, 0.01)
        assertEquals(8.0, lyrics[1].endTimeSeconds, 0.01)
    }

    @Test
    fun `parseLrc ignores non-LRC lines`() {
        val lrc = "This is not a timestamp\n[00:01.00]Valid line\n[invalid]Not valid\n[00:05.00]Another valid"
        val lyrics = LyricVideoRenderer.parseLrc(lrc)
        assertEquals(2, lyrics.size)
        assertEquals("Valid line", lyrics[0].text)
    }

    @Test
    fun `generateFrames produces correct frame count`() {
        val lyrics = listOf(LyricVideoRenderer.TimedLyric("Test", 0.0, 2.0))
        val frames = LyricVideoRenderer.generateFrames(lyrics, List(60) { 0.5f }, 2.0)
        assertEquals(60, frames.size)
        assertEquals(LyricVideoRenderer.VIDEO_WIDTH, frames[0].width)
        assertEquals(LyricVideoRenderer.VIDEO_HEIGHT, frames[0].height)
    }

    @Test
    fun `generateFrames shows current lyric during its time range`() {
        val lyrics = listOf(LyricVideoRenderer.TimedLyric("Verse", 0.0, 1.0), LyricVideoRenderer.TimedLyric("Hook", 1.0, 2.0))
        val frames = LyricVideoRenderer.generateFrames(lyrics, List(60) { 0.5f }, 2.0)
        assertEquals("Verse", frames[15].currentLyricLine)
        assertEquals("Hook", frames[45].currentLyricLine)
    }

    @Test
    fun `generateFrames uses brand colors`() {
        val frames = LyricVideoRenderer.generateFrames(emptyList(), List(30) { 0.5f }, 1.0)
        assertEquals(LyricVideoRenderer.COLOR_VOID_BLACK, frames[0].backgroundColor)
        assertEquals(LyricVideoRenderer.COLOR_ACCENT_PURPLE, frames[0].waveformColor)
        assertEquals("MACSENSE", frames[0].brandText)
    }

    @Test
    fun `computeAmplitudeEnvelope produces one value per video frame`() {
        val samples = DoubleArray(44100 * 2) { sin(it * 0.01) * 0.5 }
        val envelope = LyricVideoRenderer.computeAmplitudeEnvelope(samples, 44100, 2.0)
        assertEquals(60, envelope.size)
        envelope.forEach { assertTrue(it >= 0f && it <= 1f) }
        assertTrue(envelope.maxOrNull()!! > 0.3f)
    }

    @Test
    fun `generateFrames produces non-zero waveform amplitudes`() {
        val frames = LyricVideoRenderer.generateFrames(emptyList(), List(30) { 0.8f }, 1.0)
        assertTrue(frames[0].waveformAmplitudes.isNotEmpty())
        assertEquals(64, frames[0].waveformAmplitudes.size)
        assertTrue(frames[0].waveformAmplitudes.any { it > 0f })
    }

    @Test
    fun `createSpec combines LRC and audio into video spec`() {
        val lrc = "[00:00.00]Test lyric\n[00:02.00]Second line"
        val samples = DoubleArray(44100 * 3) { 0.5 }
        val spec = LyricVideoRenderer.createSpec(lrc, samples, 44100, 3.0)
        assertEquals(2, spec.lyrics.size)
        assertEquals(90, spec.audioAmplitudes.size)
        assertEquals("MACSENSE", spec.brandText)
        assertEquals(3.0, spec.totalDurationSeconds, 0.01)
    }
}
