package com.macsense.ai.export

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.macsense.ai.audio.PcmFileStore
import com.macsense.ai.telemetry.AppLogger
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ExportWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_FORMAT = "format"
        const val KEY_OUTPUT_FILE = "output_file"
        const val KEY_SOURCE_TAKE = "source_take_id"
        const val KEY_TIME_START = "time_start"
        const val KEY_TIME_DURATION = "time_duration"
        const val KEY_SPEED_FACTOR = "speed_factor"
        const val KEY_PRESERVE_PITCH = "preserve_pitch"
        const val KEY_REVERB_AMOUNT = "reverb_amount"
        const val KEY_INCLUDE_STEMS = "include_stems"
        const val KEY_EXCLUDE_STEMS = "exclude_stems"
        const val KEY_SOUND_DNA = "sound_dna"
        const val KEY_OUTPUT_URI = "output_uri"
        private const val TAKES_DIR = "takes"
        private const val EXPORTS_DIR = "exports"
        private const val SAMPLE_RATE = 44100

        fun createInputData(job: ExportJob): Data = workDataOf(
            KEY_JOB_ID to job.id, KEY_PROJECT_ID to job.projectId, KEY_FORMAT to job.format.name,
            KEY_OUTPUT_FILE to job.outputFileName, KEY_SOURCE_TAKE to job.sourceTakeId,
            KEY_TIME_START to (job.timeConstraint?.startSeconds ?: -1.0),
            KEY_TIME_DURATION to (job.timeConstraint?.durationSeconds ?: -1.0),
            KEY_SPEED_FACTOR to (job.tempoModulation?.speedFactor ?: -1.0),
            KEY_PRESERVE_PITCH to (job.tempoModulation?.preservePitch ?: false),
            KEY_REVERB_AMOUNT to (job.tempoModulation?.reverbAmount ?: 0.0),
            KEY_INCLUDE_STEMS to (job.includeStems?.toList()?.toTypedArray() ?: emptyArray<String>()),
            KEY_EXCLUDE_STEMS to (job.excludeStems.toList().toTypedArray()),
            KEY_SOUND_DNA to (job.soundDna ?: "")
        )

        fun enqueueBatch(context: Context, jobs: List<ExportJob>): WorkRequest? {
            if (jobs.isEmpty()) return null
            var lastRequest: WorkRequest? = null
            for (job in jobs) {
                val request = OneTimeWorkRequestBuilder<ExportWorker>().setInputData(createInputData(job)).build()
                WorkManager.getInstance(context).enqueue(request)
                lastRequest = request
            }
            return lastRequest
        }
    }

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val formatName = inputData.getString(KEY_FORMAT) ?: return Result.failure()
        val outputFileName = inputData.getString(KEY_OUTPUT_FILE) ?: return Result.failure()
        val sourceTakeId = inputData.getString(KEY_SOURCE_TAKE) ?: return Result.failure()
        val format = runCatching { ExportFormat.valueOf(formatName) }.getOrNull() ?: return Result.failure()
        if (!outputFileName.endsWith(".${format.fileExtension}", ignoreCase = true)) {
            return Result.failure(
                workDataOf(
                    "error" to "Export filename does not match ${format.displayName}: $outputFileName",
                    "format" to format.name,
                )
            )
        }
        AppLogger.i("ExportWorker", "Starting job $jobId: ${format.displayName} -> $outputFileName")
        return try {
            val context = applicationContext
            val takesDir = File(context.filesDir, TAKES_DIR)
            val exportsDir = File(context.filesDir, EXPORTS_DIR).apply { mkdirs() }
            val sourceFile = File(takesDir, "$sourceTakeId.pcm")
            if (!sourceFile.exists()) {
                AppLogger.w("ExportWorker", "Source take not found: $sourceTakeId")
                return Result.failure(workDataOf("error" to "Source take not found: $sourceTakeId"))
            }
            val store = PcmFileStore()
            val samples = store.load(sourceFile.absolutePath)
            val output = when (format) {
                ExportFormat.FULL_MIX -> samples
                ExportFormat.INSTRUMENTAL, ExportFormat.ACAPELLA, ExportFormat.STEMS_ZIP,
                ExportFormat.AAC_320, ExportFormat.MP3_320 -> {
                    return unsupported(format, "This build has no verified stem-separation, ZIP, AAC, or MP3 encoder.")
                }
                ExportFormat.TIKTOK_15S, ExportFormat.INSTAGRAM_30S -> {
                    val start = inputData.getDouble(KEY_TIME_START, 0.0).takeIf { it >= 0 } ?: 0.0
                    val duration = inputData.getDouble(KEY_TIME_DURATION, 15.0).takeIf { it >= 0 } ?: 15.0
                    ExportFactory.extractWindow(samples, SAMPLE_RATE, TimeConstraint(start, duration))
                }
                ExportFormat.SLOWED_REVERB, ExportFormat.SPED_UP -> {
                    val speedFactor = inputData.getDouble(KEY_SPEED_FACTOR, 1.0).takeIf { it > 0 } ?: 1.0
                    val preservePitch = inputData.getBoolean(KEY_PRESERVE_PITCH, false)
                    val reverbAmount = inputData.getDouble(KEY_REVERB_AMOUNT, 0.0)
                    ExportFactory.applyTempoModulation(samples, SAMPLE_RATE, TempoModulation(speedFactor, preservePitch, reverbAmount))
                }
            }
            val outputFile = File(exportsDir, outputFileName)
            writeWav(output, outputFile)
            val soundDna = inputData.getString(KEY_SOUND_DNA)
            if (!soundDna.isNullOrEmpty()) {
                File(exportsDir, "$outputFileName.dna").writeText(soundDna)
                AppLogger.i("ExportWorker", "Wrote Sound DNA: $outputFileName.dna")
            }
            AppLogger.i("ExportWorker", "Completed job $jobId: ${output.size} samples -> ${outputFile.absolutePath}")
            Result.success(workDataOf(KEY_OUTPUT_URI to outputFile.toURI().toString(), "job_id" to jobId, "format" to formatName))
        } catch (e: Exception) {
            AppLogger.e("ExportWorker", "Export job $jobId failed: ${e.message}", e)
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }

    private fun unsupported(format: ExportFormat, detail: String): Result {
        val message = "${format.displayName} is unavailable: $detail"
        AppLogger.w("ExportWorker", message)
        return Result.failure(workDataOf("error" to message, "format" to format.name, "unsupported" to true))
    }

    /** Writes a real mono 16-bit PCM WAV file. The extension/format declaration can no longer lie. */
    private fun writeWav(samples: DoubleArray, destination: File) {
        require(destination.extension.equals("wav", ignoreCase = true)) {
            "WAV writer cannot create a non-WAV file: ${destination.name}"
        }
        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { output ->
            val dataBytes = samples.size * 2
            writeAscii(output, "RIFF")
            writeLeInt(output, 36 + dataBytes)
            writeAscii(output, "WAVEfmt ")
            writeLeInt(output, 16)
            writeLeShort(output, 1)
            writeLeShort(output, 1)
            writeLeInt(output, SAMPLE_RATE)
            writeLeInt(output, SAMPLE_RATE * 2)
            writeLeShort(output, 2)
            writeLeShort(output, 16)
            writeAscii(output, "data")
            writeLeInt(output, dataBytes)
            samples.forEach { sample ->
                writeLeShort(output, (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt())
            }
        }
    }

    private fun writeAscii(output: OutputStream, value: String) = output.write(value.encodeToByteArray())
    private fun writeLeInt(output: OutputStream, value: Int) =
        output.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array())
    private fun writeLeShort(output: OutputStream, value: Int) =
        output.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array())
}
