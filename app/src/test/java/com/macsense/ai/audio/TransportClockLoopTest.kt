package com.macsense.ai.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransportClockLoopTest {

    @Test
    fun `advance wraps inside loop region`() {
        var now = 0L
        val clock = TransportClock { now }
        clock.setBpm(120.0) // bar = 2s at 120bpm (4 beats)
        clock.start()
        clock.setLoopRegion(2, 4)

        val barNanos = 2_000_000_000L
        now = 5 * barNanos // absolute bar 5
        // loop [2,4): 5 -> 2 + (5-2) % 2 = 3
        assertEquals(3, clock.advance())

        now = 6 * barNanos // absolute bar 6 -> 2 + (6-2)%2 = 2
        assertEquals(2, clock.advance())
    }

    @Test
    fun `bars before the loop end play through untouched`() {
        var now = 0L
        val clock = TransportClock { now }
        clock.setBpm(120.0)
        clock.start()
        clock.setLoopRegion(2, 4)
        now = 1 * 2_000_000_000L
        assertEquals(1, clock.advance())
    }

    @Test
    fun `clearing the loop restores linear playback`() {
        var now = 0L
        val clock = TransportClock { now }
        clock.setBpm(120.0)
        clock.start()
        clock.setLoopRegion(0, 2)
        clock.setLoopRegion(null, null)
        assertNull(clock.loopStartBar)
        now = 5 * 2_000_000_000L
        assertEquals(5, clock.advance())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `degenerate loop region is rejected`() {
        TransportClock().setLoopRegion(3, 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative loop start is rejected`() {
        TransportClock().setLoopRegion(-1, 2)
    }
}
