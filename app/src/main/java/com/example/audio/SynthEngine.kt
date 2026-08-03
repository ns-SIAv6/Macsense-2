package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.model.SoundGenome
import com.example.data.model.SoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural PCM Audio Synthesizer Engine for MA¢SENSE.
 * Synthesizes real audio buffers using 4D SoundGenome parameters & Chromosomes.
 */
class SynthEngine {
    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playGenome(genome: SoundGenome) {
        scope.launch {
            val pcmData = generatePcmForGenome(genome)
            playPcmBuffer(pcmData)
        }
    }

    fun playNote(frequencyHz: Float, durationMs: Int = 300, type: SoundType = SoundType.SYNTH) {
        scope.launch {
            val numSamples = (sampleRate * (durationMs / 1000.0f)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = exp(-3.0 * t * (1000.0 / durationMs))
                val rawWave = when (type) {
                    SoundType.SUB_808 -> sin(2.0 * PI * frequencyHz * t) + 0.3 * sin(2.0 * PI * (frequencyHz * 0.5) * t)
                    SoundType.KICK -> sin(2.0 * PI * (frequencyHz * exp(-15.0 * t)) * t)
                    SoundType.SNARE -> 0.4 * sin(2.0 * PI * frequencyHz * t) + 0.6 * (Math.random() * 2.0 - 1.0)
                    SoundType.HIHAT -> (Math.random() * 2.0 - 1.0) * exp(-25.0 * t)
                    SoundType.SYNTH -> sin(2.0 * PI * frequencyHz * t) + 0.5 * sin(2.0 * PI * frequencyHz * 2 * t)
                    SoundType.PAD -> sin(2.0 * PI * frequencyHz * t) + 0.5 * sin(2.0 * PI * (frequencyHz * 1.01) * t)
                    SoundType.FX -> sin(2.0 * PI * (frequencyHz + 500 * sin(2.0 * PI * 10 * t)) * t)
                }
                val sampleValue = (rawWave * envelope * 24000.0).coerceIn(-32767.0, 32767.0)
                buffer[i] = sampleValue.toInt().toShort()
            }
            playPcmBuffer(buffer)
        }
    }

    private fun generatePcmForGenome(genome: SoundGenome): ShortArray {
        // Duration from chrom2 (decay ms) or mass
        val durationMs = (genome.chrom2 + (genome.mass * 500)).toInt().coerceIn(100, 2000)
        val numSamples = (sampleRate * (durationMs / 1000.0f)).toInt()
        val buffer = ShortArray(numSamples)

        val baseFreq = genome.chrom1.toDouble().coerceIn(30.0, 1500.0)
        val distortion = genome.chrom5 / 100.0
        val entropyFactor = genome.entropy.toDouble()

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / (durationMs / 1000.0)
            val envelope = exp(-4.0 * progress)

            // Frequency envelope (radiance adds pitch rise/fall)
            val freqEnv = baseFreq * (1.0 + genome.radiance * exp(-10.0 * progress))

            var wave = sin(2.0 * PI * freqEnv * t)
            // Curvature adds harmonic overtones
            if (genome.curvature > 0.2f) {
                wave += 0.4 * sin(2.0 * PI * freqEnv * 2.0 * t * (1.0 + genome.curvature))
            }

            // Entropy adds stochastic noise distortion
            if (entropyFactor > 0.3) {
                val noise = (Math.random() * 2.0 - 1.0) * (entropyFactor - 0.2)
                wave = (1.0 - entropyFactor) * wave + noise
            }

            // Distortion clipping
            if (distortion > 0.1) {
                wave = (wave * (1.0 + distortion * 3.0)).coerceIn(-1.0, 1.0)
            }

            // Apply scar distortion if resurrected
            if (genome.scarMagnitude > 0f) {
                wave += 0.15 * sin(2.0 * PI * 60.0 * t) * genome.scarMagnitude
            }

            val finalVal = (wave * envelope * 26000.0).coerceIn(-32767.0, 32767.0)
            buffer[i] = finalVal.toInt().toShort()
        }
        return buffer
    }

    private fun playPcmBuffer(buffer: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            // Release track after playback completes
            scope.launch {
                kotlinx.coroutines.delay((buffer.size * 1000L / sampleRate) + 100)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
