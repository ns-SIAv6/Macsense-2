package com.macsense.ai.export

import com.macsense.ai.audio.SoundGenome
import kotlin.math.min
import kotlin.math.sin

object ExportFactory {
    val STEM_LANES = listOf("808/Bass", "Kick", "Snare", "Hi-Hat", "Clap", "Percussion", "Riser", "Crash", "Bass Synth", "Lead", "Pads", "Vocal/Adlib")
    val VOCAL_LANES = setOf("Vocal/Adlib")
    val INSTRUMENTAL_LANES = STEM_LANES.toSet() - VOCAL_LANES

    fun createBatch(projectId: String, projectName: String, sourceTakeId: String, genome: SoundGenome? = null, trackDurationSeconds: Double = 180.0, creatorName: String = "MacSense Artist", tags: Set<String> = emptySet()): List<ExportJob> {
        val safeName = projectName.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val soundDna = genome?.let { g ->
            GenomeShareableTrack.toShareableJson(GenomeShareableTrack(genome = g, trackName = projectName, creatorName = creatorName, exportedAt = System.currentTimeMillis(), tags = tags.toList(), lineageSummary = "Parents: ${g.parents.size}, Source: ${g.sourceId}"))
        }
        val jobs = mutableListOf<ExportJob>()
        jobs += ExportJob(id = "${safeName}_full_mix", projectId = projectId, format = ExportFormat.FULL_MIX, outputFileName = "${safeName}_full_mix.wav", sourceTakeId = sourceTakeId, soundDna = soundDna)
        // Stem-dependent formats, ZIP, AAC, and MP3 are intentionally omitted until
        // real renderers/encoders are bundled. ExportWorker also rejects direct jobs for
        // them, preventing a caller from creating mislabeled PCM files.
        val hookStart = findMostEnergeticWindow(trackDurationSeconds)
        jobs += ExportJob(id = "${safeName}_tiktok", projectId = projectId, format = ExportFormat.TIKTOK_15S, outputFileName = "${safeName}_tiktok_15s.wav", sourceTakeId = sourceTakeId, timeConstraint = TimeConstraint(hookStart, 15.0), soundDna = soundDna)
        jobs += ExportJob(id = "${safeName}_instagram", projectId = projectId, format = ExportFormat.INSTAGRAM_30S, outputFileName = "${safeName}_instagram_30s.wav", sourceTakeId = sourceTakeId, timeConstraint = TimeConstraint(hookStart, 30.0))
        jobs += ExportJob(id = "${safeName}_slowed", projectId = projectId, format = ExportFormat.SLOWED_REVERB, outputFileName = "${safeName}_slowed_reverb.wav", sourceTakeId = sourceTakeId, tempoModulation = TempoModulation(0.85, false, 0.6), soundDna = soundDna)
        // The current renderer uses resampling, which changes pitch. Do not claim
        // pitch preservation until a real time-stretch processor is bundled.
        jobs += ExportJob(id = "${safeName}_sped_up", projectId = projectId, format = ExportFormat.SPED_UP, outputFileName = "${safeName}_sped_up.wav", sourceTakeId = sourceTakeId, tempoModulation = TempoModulation(1.25, false), soundDna = soundDna)
        return jobs
    }

    fun findMostEnergeticWindow(trackDurationSeconds: Double): Double {
        val hookStart = trackDurationSeconds * 0.30
        return min(hookStart, maxOf(0.0, trackDurationSeconds - 30.0))
    }

    fun applyTempoModulation(samples: DoubleArray, sampleRate: Int, modulation: TempoModulation): DoubleArray {
        val outputLength = (samples.size / modulation.speedFactor).toInt()
        val output = DoubleArray(outputLength)
        for (i in 0 until outputLength) {
            val sourceIndex = i * modulation.speedFactor
            val index0 = sourceIndex.toInt()
            val index1 = min(index0 + 1, samples.lastIndex)
            val fraction = sourceIndex - index0
            output[i] = samples[index0] * (1.0 - fraction) + samples[index1] * fraction
        }
        if (modulation.reverbAmount > 0.0) return applyReverb(output, sampleRate, modulation.reverbAmount)
        return output
    }

    fun applyReverb(samples: DoubleArray, sampleRate: Int, amount: Double): DoubleArray {
        val delaySamples = (sampleRate * 120 / 1000.0).toInt()
        val feedback = 0.4 * amount
        val output = DoubleArray(samples.size)
        val delayBuffer = DoubleArray(delaySamples)
        var delayIndex = 0
        var lowpassPrev = 0.0
        val lowpassAlpha = 0.3
        for (i in samples.indices) {
            val delayed = delayBuffer[delayIndex]
            val lowpassed = delayed * (1.0 - lowpassAlpha) + lowpassPrev * lowpassAlpha
            lowpassPrev = lowpassed
            output[i] = samples[i] + lowpassed * amount
            delayBuffer[delayIndex] = samples[i] + lowpassed * feedback
            delayIndex = (delayIndex + 1) % delaySamples
        }
        return output
    }

    fun extractWindow(samples: DoubleArray, sampleRate: Int, constraint: TimeConstraint): DoubleArray {
        val startSample = (constraint.startSeconds * sampleRate).toInt()
        val lengthSamples = (constraint.durationSeconds * sampleRate).toInt()
        val endSample = min(startSample + lengthSamples, samples.size)
        if (startSample >= samples.size) return DoubleArray(0)
        val window = samples.copyOfRange(startSample, endSample)
        val fadeSamples = min(sampleRate / 100, window.size / 4)
        for (i in 0 until fadeSamples) { val gain = i.toDouble() / fadeSamples; window[i] *= gain }
        for (i in 0 until fadeSamples) { val gain = i.toDouble() / fadeSamples; window[window.size - 1 - i] *= gain }
        return window
    }
}
