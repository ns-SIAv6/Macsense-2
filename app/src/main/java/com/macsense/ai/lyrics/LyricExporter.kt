package com.macsense.ai.lyrics

/**
 * P7 (issue #42): lyric export formats — plain text, LRC (timestamped, feeds the lyric
 * video pipeline) and SRT (captions). Timing derives from the DAW: each line occupies a
 * fixed number of bars at the project BPM (4 beats/bar).
 */
object LyricExporter {

    data class TimedLine(val startMs: Long, val endMs: Long, val text: String)

    /** Assigns bar-quantized timestamps to lines from BPM + bars-per-line. */
    fun timeLines(lines: List<String>, bpm: Double, barsPerLine: Int = 1): List<TimedLine> {
        require(bpm > 0) { "bpm must be positive" }
        require(barsPerLine > 0) { "barsPerLine must be positive" }
        val barMs = (240_000.0 / bpm) * barsPerLine
        return lines.filter { it.isNotBlank() }.mapIndexed { i, line ->
            TimedLine(
                startMs = (i * barMs).toLong(),
                endMs = ((i + 1) * barMs).toLong(),
                text = line.trim(),
            )
        }
    }

    fun toPlainText(lines: List<String>): String = lines.joinToString("\n").trim()

    fun toLrc(timed: List<TimedLine>, title: String? = null, artist: String? = null): String {
        val sb = StringBuilder()
        title?.let { sb.appendLine("[ti:$it]") }
        artist?.let { sb.appendLine("[ar:$it]") }
        timed.forEach { line ->
            sb.appendLine("[${lrcStamp(line.startMs)}]${line.text}")
        }
        return sb.toString().trimEnd()
    }

    fun toSrt(timed: List<TimedLine>): String {
        val sb = StringBuilder()
        timed.forEachIndexed { i, line ->
            sb.appendLine(i + 1)
            sb.appendLine("${srtStamp(line.startMs)} --> ${srtStamp(line.endMs)}")
            sb.appendLine(line.text)
            sb.appendLine()
        }
        return sb.toString().trimEnd()
    }

    private fun lrcStamp(ms: Long): String {
        val m = ms / 60_000
        val s = (ms % 60_000) / 1000
        val cs = (ms % 1000) / 10
        return "%02d:%02d.%02d".format(m, s, cs)
    }

    private fun srtStamp(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        val s = (ms % 60_000) / 1000
        val milli = ms % 1000
        return "%02d:%02d:%02d,%03d".format(h, m, s, milli)
    }
}
