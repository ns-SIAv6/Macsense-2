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

    /** Phase 4 loop region (issue #39): when set, playback wraps inside [loopStartBar, loopEndBar). */
    var loopStartBar: Int? = null
        private set
    var loopEndBar: Int? = null
        private set

    private var startNanos: Long = 0L

    /**
     * Sets (or clears, when either is null) the loop region. Start is inclusive, end exclusive;
     * the region must be at least one bar long and non-negative.
     */
    fun setLoopRegion(startBar: Int?, endBar: Int?) {
        if (startBar == null || endBar == null) {
            loopStartBar = null
            loopEndBar = null
            return
        }
        require(startBar >= 0 && endBar > startBar) {
            "Invalid loop region [$startBar, $endBar): start must be >= 0 and end > start"
        }
        loopStartBar = startBar
        loopEndBar = endBar
    }

    /** Maps an absolute elapsed bar count into the loop region, if one is active. */
    fun applyLoop(absoluteBar: Int): Int {
        val start = loopStartBar ?: return absoluteBar
        val end = loopEndBar ?: return absoluteBar
        if (absoluteBar < end) return absoluteBar
        val length = end - start
        return start + (absoluteBar - start) % length
    }

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

    /**
     * Advances to the bar that wall-clock time indicates we should be on, wrapped into the
     * active loop region when one is set. Safe to call repeatedly.
     */
    fun advance(): Int {
        barIndex = applyLoop(expectedBarIndex())
        return barIndex
    }

    private fun max(a: Long, b: Long) = if (a > b) a else b

    companion object {
        private const val BAR_DURATION_MS_AT_1_BPM = 240_000.0
    }
}
