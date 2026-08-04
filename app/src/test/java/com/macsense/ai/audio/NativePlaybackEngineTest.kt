package com.macsense.ai.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * In the JVM unit test environment there's no Android native runtime, so the native
 * `macsense_playback` library can never actually load. These tests assert the wrapper
 * degrades to safe no-ops in that case rather than throwing UnsatisfiedLinkError, which is
 * the behavior production code (DawViewModel) depends on when running headless/in tests.
 */
class NativePlaybackEngineTest {

    @Test
    fun `engine reports native unavailable in JVM test environment`() {
        val engine = NativePlaybackEngine()
        assertFalse(engine.isNativeAvailable)
    }

    @Test
    fun `all operations are safe no-ops when native lib unavailable`() {
        val engine = NativePlaybackEngine()

        assertFalse(engine.load(DoubleArray(100) { 0.1 }, 44100))
        assertFalse(engine.play())
        assertFalse(engine.pause())
        assertFalse(engine.stop())
        engine.seekToFrame(50)
        assertEquals(0L, engine.positionFrames)
        assertEquals(0L, engine.totalFrames)
        assertEquals(0.0, engine.positionSeconds(44100), 0.0001)

        engine.close()
    }

    @Test
    fun `close is idempotent and safe to call multiple times`() {
        val engine = NativePlaybackEngine()
        engine.close()
        engine.close()
        assertTrue(true)
    }
}
