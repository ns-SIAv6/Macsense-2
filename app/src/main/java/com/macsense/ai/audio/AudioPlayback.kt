package com.macsense.ai.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.roundToInt

/** Plays normalized mono PCM without adding a media dependency. */
class AudioPlayback(private val sampleRate: Int = AudioCapture.DEFAULT_SAMPLE_RATE) : AutoCloseable {
    private var track: AudioTrack? = null

    @Synchronized
    fun play(samples: DoubleArray) {
        stop()
        require(samples.all { it in -1.0..1.0 }) { "PCM samples must be normalized" }
        val pcm = ShortArray(samples.size) { (samples[it].coerceIn(-1.0, 1.0) * 32767.0).roundToInt().toShort() }
        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, CHANNEL, ENCODING)
        require(minBuffer > 0) { "Audio output is unavailable" }
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(ENCODING).setChannelMask(CHANNEL).build())
            .setBufferSizeInBytes(maxOf(minBuffer, pcm.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audioTrack.write(pcm, 0, pcm.size)
        audioTrack.play()
        track = audioTrack
    }

    @Synchronized fun stop() { track?.let { runCatching { it.stop() }; it.release() }; track = null }
    override fun close() = stop()

    companion object { private const val CHANNEL = AudioFormat.CHANNEL_OUT_MONO; private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT }
}
