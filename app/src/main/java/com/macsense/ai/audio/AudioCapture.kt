package com.macsense.ai.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.max

/** Captures microphone input as normalized mono PCM samples. */
class AudioCapture(
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE
) : AutoCloseable {
    private var recorder: AudioRecord? = null
    private var captureThread: Thread? = null
    private val samples = ArrayList<Double>()
    @Volatile private var capturing = false

    /**
     * Starts capturing microphone audio in the background.
     *
     * @param onChunk optional callback invoked from the capture thread with each freshly-read
     *   chunk of normalized samples, in addition to the samples being appended to the buffer
     *   returned by [stop]. Useful for live consumers (meters, spectrum analyzers) that need
     *   audio as it arrives rather than waiting for capture to stop.
     * @return `true` if capture starts successfully, `false` if capture is already active or initialization fails.
     */
    @Synchronized
    @Suppress("MissingPermission")
    fun start(onChunk: ((DoubleArray) -> Unit)? = null): Boolean {
        if (capturing) return false
        return try {
            val minimumBuffer = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minimumBuffer <= 0) return false

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minimumBuffer, sampleRate / 5)
            )
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release()
                return false
            }

            samples.clear()
            recorder = audioRecord
            capturing = true
            captureThread = Thread {
                val buffer = ShortArray(max(256, sampleRate / 20))
                try {
                    audioRecord.startRecording()
                    while (capturing) {
                        val count = audioRecord.read(buffer, 0, buffer.size)
                        if (count > 0) {
                            val chunk = DoubleArray(count)
                            for (index in 0 until count) chunk[index] = buffer[index] / SHORT_SCALE
                            synchronized(samples) { for (v in chunk) samples += v }
                            onChunk?.invoke(chunk)
                        }
                    }
                } finally {
                    runCatching { audioRecord.stop() }
                    audioRecord.release()
                }
            }.also { it.start() }
            true
        } catch (e: Throwable) {
            false
        }
    }

    @Synchronized
    fun stop(): DoubleArray {
        capturing = false
        captureThread?.join(CAPTURE_THREAD_JOIN_MS)
        captureThread = null
        recorder = null
        return synchronized(samples) { samples.toDoubleArray() }
    }

    override fun close() { stop() }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 44_100
        private const val SHORT_SCALE = 32_768.0
        private const val CAPTURE_THREAD_JOIN_MS = 1_000L
    }
}
