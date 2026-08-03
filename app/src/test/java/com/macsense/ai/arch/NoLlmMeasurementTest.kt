package com.macsense.ai.arch

import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class NoLlmMeasurementTest {
    @Test
    fun verifyNoLlmInDsp() {
        val dspDir = File("src/main/java/com/macsense/ai/dsp")
        if (!dspDir.exists()) {
            // Might be running from root directory
            if (File("app/src/main/java/com/macsense/ai/dsp").exists()) {
                verifyDir(File("app/src/main/java/com/macsense/ai/dsp"))
                return
            }
            fail("DSP directory not found")
        }
        verifyDir(dspDir)
    }
    
    private fun verifyDir(dir: File) {
        val forbidden = listOf("http", "okhttp", "Retrofit", "Gemini", "generativeai", "import android")
        dir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            for (f in forbidden) {
                if (text.contains(f)) {
                    fail("File ${file.name} contains forbidden token: $f")
                }
            }
            // Also check for suspend fun that might be IO
            val lines = text.lines()
            for (line in lines) {
                if (line.contains("suspend fun") && (line.contains("Network") || line.contains("Io") || line.contains("http"))) {
                    fail("File ${file.name} contains suspend fun doing IO")
                }
            }
        }
    }
}
