package com.macsense.ai.audio

/**
 * Kotlin wrapper around the native (AAudio/C++) low-latency playback engine.
 *
 * Loads a full track of normalized PCM up front (see [PlaybackEngine] native docs for why this
 * is appropriate for short recorded takes/loops rather than long-form streaming), then drives
 * playback via a lock-free audio callback on the native side. All public methods here are safe
 * to call from any thread; the native layer only touches the audio-thread-owned read cursor from
 * its own callback.
 *
 * Falls back gracefully (methods return `false`/no-op) if the native library failed to load,
 * e.g. in JVM unit tests where no Android native libraries are present.
 */
class NativePlaybackEngine : AutoCloseable {
    private val handle: Long = if (nativeLibAvailable) runCatching { nativeCreate() }.getOrDefault(0L) else 0L
    private var released = false

    val isNativeAvailable: Boolean get() = handle != 0L

    /** Loads normalized mono PCM (values expected in [-1, 1]) at the given sample rate. */
    fun load(samples: DoubleArray, sampleRate: Int): Boolean {
        if (!isNativeAvailable || released) return false
        val floats = FloatArray(samples.size) { samples[it].toFloat() }
        return nativeLoadPcm(handle, floats, sampleRate)
    }

    fun play(): Boolean = if (isNativeAvailable && !released) nativePlay(handle) else false

    fun pause(): Boolean = if (isNativeAvailable && !released) nativePause(handle) else false

    fun stop(): Boolean = if (isNativeAvailable && !released) nativeStop(handle) else false

    fun seekToFrame(frame: Long) {
        if (isNativeAvailable && !released) nativeSeekToFrame(handle, frame)
    }

    val positionFrames: Long get() = if (isNativeAvailable && !released) nativeGetPositionFrames(handle) else 0L

    val totalFrames: Long get() = if (isNativeAvailable && !released) nativeGetTotalFrames(handle) else 0L

    /** Playback position in seconds, derived from [positionFrames] and the loaded sample rate. */
    fun positionSeconds(sampleRate: Int): Double =
        if (sampleRate > 0) positionFrames.toDouble() / sampleRate else 0.0

    @Synchronized
    override fun close() {
        if (!released && isNativeAvailable) {
            nativeRelease(handle)
        }
        released = true
    }

    private external fun nativeCreate(): Long
    private external fun nativeLoadPcm(handle: Long, samples: FloatArray, sampleRate: Int): Boolean
    private external fun nativePlay(handle: Long): Boolean
    private external fun nativePause(handle: Long): Boolean
    private external fun nativeStop(handle: Long): Boolean
    private external fun nativeSeekToFrame(handle: Long, frame: Long)
    private external fun nativeGetPositionFrames(handle: Long): Long
    private external fun nativeGetTotalFrames(handle: Long): Long
    private external fun nativeRelease(handle: Long)

    companion object {
        private const val LIB_NAME = "macsense_playback"

        // Loaded once per process. In JVM unit tests (no Android runtime), this will fail and
        // nativeLibAvailable stays false, so every instance degrades to a harmless no-op rather
        // than crashing the test process with an UnsatisfiedLinkError.
        val nativeLibAvailable: Boolean = runCatching { System.loadLibrary(LIB_NAME) }.isSuccess
    }
}
