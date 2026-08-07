package com.macsense.ai.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricExporterTest {

    private val lines = listOf("First line here", "Second line flows", "")

    @Test
    fun `timing quantizes lines to bars at project bpm`() {
        // 120 bpm -> one 4-beat bar = 2000ms
        val timed = LyricExporter.timeLines(lines, bpm = 120.0)
        assertEquals(2, timed.size) // blank line dropped
        assertEquals(0L, timed[0].startMs)
        assertEquals(2000L, timed[0].endMs)
        assertEquals(2000L, timed[1].startMs)
        assertEquals(4000L, timed[1].endMs)
    }

    @Test
    fun `lrc output carries metadata and centisecond stamps`() {
        val timed = LyricExporter.timeLines(lines, bpm = 120.0)
        val lrc = LyricExporter.toLrc(timed, title = "Night Drive", artist = "MacSense")
        assertTrue(lrc.startsWith("[ti:Night Drive]"))
        assertTrue(lrc.contains("[ar:MacSense]"))
        assertTrue(lrc.contains("[00:00.00]First line here"))
        assertTrue(lrc.contains("[00:02.00]Second line flows"))
    }

    @Test
    fun `srt output has indices and millisecond ranges`() {
        val timed = LyricExporter.timeLines(lines, bpm = 120.0)
        val srt = LyricExporter.toSrt(timed)
        assertTrue(srt.startsWith("1\n00:00:00,000 --> 00:00:02,000\nFirst line here"))
        assertTrue(srt.contains("2\n00:00:02,000 --> 00:00:04,000\nSecond line flows"))
    }

    @Test
    fun `plain text round trip`() {
        assertEquals("a\nb", LyricExporter.toPlainText(listOf("a", "b")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-positive bpm fails loudly`() {
        LyricExporter.timeLines(lines, bpm = 0.0)
    }
}
