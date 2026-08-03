package com.macsense.ai.audio

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException

/** Stores normalized mono samples without lossy conversion. */
class PcmFileStore {
    fun save(samples: DoubleArray, path: String) {
        require(samples.all { it in -1.0..1.0 }) { "PCM samples must be normalized" }
        val target = File(path)
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "${target.name}.tmp")
        DataOutputStream(BufferedOutputStream(temp.outputStream())).use { out ->
            out.writeInt(MAGIC)
            out.writeInt(samples.size)
            samples.forEach(out::writeDouble)
        }
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IOException("Unable to commit PCM file: $path")
        }
    }

    fun load(path: String): DoubleArray {
        DataInputStream(BufferedInputStream(File(path).inputStream())).use { input ->
            check(input.readInt() == MAGIC) { "Unsupported PCM file: $path" }
            val size = input.readInt()
            require(size in 0..MAX_SAMPLES) { "Invalid PCM sample count: $size" }
            return DoubleArray(size) {
                input.readDouble().also { value ->
                    require(value in -1.0..1.0) { "Invalid PCM sample" }
                }
            }
        }
    }

    private companion object {
        const val MAGIC = 0x4D43504D
        const val MAX_SAMPLES = 48_000 * 60 * 60
    }
}
