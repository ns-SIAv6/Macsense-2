package com.macsense.ai.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportClockTest {

    private class FakeNanoClock(startAt: Long = 0L) {
        var nowNanos: Long = startAt
        fun advanceMs(ms: Long) { nowNanos += ms * 1_000_000L }
        val reader: () -> Long = { nowNanos }
    }

    @Test
    fun `advance tracks elapsed time without compounding drift`() {
        val fake = FakeNanoClock()
        val clock = TransportClock(nowNanos = fake.reader)
        clock.start()
        clock.setBpm(120.0) // 2 sec/bar

        repeat(50) {
            fake.advanceMs(2000)
            clock.advance()
        }

        assertEquals(50, clock.barIndex)
    }

    @Test
    fun `advance handles irregular polling intervals without losing bars`() {
        val fake = FakeNanoClock()
        val clock = TransportClock(nowNanos = fake.reader)
        clock.start()
        clock.setBpm(120.0) // 2 sec/bar

        fake.advanceMs(700)
        clock.advance()
        assertEquals(0, clock.barIndex)

        fake.advanceMs(1600) // total 2300ms -> bar 1
        clock.advance()
        assertEquals(1, clock.barIndex)

        fake.advanceMs(9700) // total 12000ms -> bar 6
        clock.advance()
        assertEquals(6, clock.barIndex)
    }

    @Test
    fun `nextBarDelayMs returns zero once bar boundary has passed`() {
        val fake = FakeNanoClock()
        val clock = TransportClock(nowNanos = fake.reader)
        clock.start()
        clock.setBpm(120.0)

        fake.advanceMs(5000) // well past bar 0's boundary
        assertEquals(0L, clock.nextBarDelayMs())
    }

    @Test
    fun `setBpm mid-playback reanchors without jumping bar index`() {
        val fake = FakeNanoClock()
        val clock = TransportClock(nowNanos = fake.reader)
        clock.start()
        clock.setBpm(120.0)

        fake.advanceMs(2000)
        clock.advance()
        assertEquals(1, clock.barIndex)

        clock.setBpm(240.0) // tempo doubles
        assertEquals(1, clock.barIndex)

        fake.advanceMs(1000) // one bar at the new, faster tempo
        clock.advance()
        assertEquals(2, clock.barIndex)
    }

    @Test
    fun `stop freezes bar index`() {
        val fake = FakeNanoClock()
        val clock = TransportClock(nowNanos = fake.reader)
        clock.start()
        clock.setBpm(120.0)
        fake.advanceMs(2000)
        clock.advance()
        clock.stop()

        fake.advanceMs(10000)
        assertEquals(1, clock.barIndex)
        assertTrue(!clock.isPlaying)
    }
}
