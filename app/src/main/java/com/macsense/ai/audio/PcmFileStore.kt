package com.macsense.ai.audio

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/** Small dependency-free store for normalized mono PCM samples. */
class PcmFileStore {
    fun save(samples: DoubleArray, path: String) {
        require(samples.all { it in -1.0..1.0 }) { "PCM samples must be normalized" }
        val target = File(path)
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        DataOutputStream(FileOutputStream(temporary)).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeInt(samples.size)
            samples.forEach(output::writeDouble)
        }
        if (!temporary.renameTo(target)) {
            temporary.delete()
            throw IOException("Unable to commit PCM file: $path")
        }
    }

    fun load(path: String): DoubleArray {
        DataInputStream(FileInputStream(path)).use { input ->
            if (input.readInt() != MAGIC || input.readInt() != VERSION) throw IOException("Invalid PCM file")
            val count = input.readInt()
            if (count < 0 || count > MAX_SAMPLES) throw IOException("Invalid PCM sample count")
            return DoubleArray(count) {
                input.readDouble().also { value ->
                    if (value !in -1.0..1.0) throw IOException("Invalid PCM sample")
                }
            }
        }
    }

    companion object {
        private const val MAGIC = 0x4D43504D
        private const val VERSION = 1
        private const val MAX_SAMPLES = 44_100 * 60 * 60
    }
}
