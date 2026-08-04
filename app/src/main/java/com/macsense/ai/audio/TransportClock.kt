package com.macsense.ai.audio

/**
 * Bar-advancing transport clock with drift correction.
 *
 * The previous implementation scheduled `delay(barDurationMs)` in a loop, which accumulates
 * scheduling error (coroutine dispatch jitter, GC pauses) on every iteration -- over a long
 * playback session the reported bar position drifts noticeably out of sync with wall-clock time.
 *
 * This version tracks an absolute reference time (`startNanos`) and a target bar count, and
 * computes each bar's *absolute* deadline from that fixed origin rather than chaining relative
 * delays. Callers should use [nextBarDelayMs] to know how long to sleep/delay before the next
 * bar boundary, then call [advance] once elapsed -- drift never compounds because every deadline
 * is recomputed from the same origin.
 */
class TransportClock(private val nowNanos: () -> Long = { System.nanoTime() }) {
    var isPlaying = false
        private set
    var bpm = 120.0
        private set
    var barIndex = 0
        private set

    private var startNanos: Long = 0L

    fun start() {
        startNanos = nowNanos()
        barIndex = 0
        isPlaying = true
    }

    fun stop() {
        isPlaying = false
    }

    fun setBpm(newBpm: Double) {
        require(newBpm > 0) { "bpm must be positive" }
        if (isPlaying) {
            // Re-anchor so the bar we're currently in keeps its elapsed fraction rather than
            // jumping backwards/forwards when tempo changes mid-playback.
            val elapsedBars = barIndex
            bpm = newBpm
            startNanos = nowNanos() - (elapsedBars * barDurationNanos())
        } else {
            bpm = newBpm
        }
    }

    private fun barDurationNanos(): Long = (BAR_DURATION_MS_AT_1_BPM / bpm * 1_000_000.0).toLong()

    /** Absolute bar index that *should* be current given elapsed wall-clock time since [start]. */
    fun expectedBarIndex(): Int {
        if (!isPlaying) return barIndex
        val elapsedNanos = nowNanos() - startNanos
        return (elapsedNanos / barDurationNanos()).toInt()
    }

    /** Milliseconds to wait before the next bar boundary, correcting for any accumulated drift. */
    fun nextBarDelayMs(): Long {
        if (!isPlaying) return 0L
        val nextBarNanos = startNanos + (barIndex + 1) * barDurationNanos()
        val remainingNanos = nextBarNanos - nowNanos()
        return max(0L, remainingNanos / 1_000_000L)
    }

    /** Advances to the bar that wall-clock time indicates we should be on. Safe to call repeatedly. */
    fun advance(): Int {
        barIndex = expectedBarIndex()
        return barIndex
    }

    private fun max(a: Long, b: Long) = if (a > b) a else b

    companion object {
        private const val BAR_DURATION_MS_AT_1_BPM = 240_000.0
    }
}
