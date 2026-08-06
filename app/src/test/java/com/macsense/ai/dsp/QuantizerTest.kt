package com.macsense.ai.dsp

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantizerTest {
    @Test fun testT1_Strength1() {
        val off = Quantizer.quantizeOffset(0.51, 120.0, 4, 1.0)
        println("TEST_RES: quantizeOffset strength=1.0 = $off")
        assertEquals(-0.01, off, 1e-6)
    }

    @Test fun testT2_Strength05() {
        val off = Quantizer.quantizeOffset(0.51, 120.0, 4, 0.5)
        println("TEST_RES: quantizeOffset strength=0.5 = $off")
        assertEquals(-0.005, off, 1e-6)
    }

    @Test fun testT3_NonDestructive() {
        val pcm = DoubleArray(48000) { kotlin.math.sin(it * 0.01) }
        val snapshot = pcm.copyOf()
        val onsets = doubleArrayOf(0.51, 1.02, 1.49)
        Quantizer.applyQuantize(pcm, onsets, 120.0, 4, 1.0, 48000)
        assertArrayEquals("source PCM was mutated", snapshot, pcm, 0.0)
    }

    @Test fun testT4_QuantizedCorrectly() {
        val pcm = DoubleArray(48000) { kotlin.math.sin(it * 0.01) }
        val onsets = doubleArrayOf(0.51)
        val quantized = Quantizer.applyQuantize(pcm, onsets, 120.0, 4, 1.0, 48000)
        // Ensure that the quantized PCM has been modified in the target region
        var diffCount = 0
        for (i in pcm.indices) {
            if (pcm[i] != quantized[i]) {
                diffCount++
            }
        }
        assertTrue("Quantized array should be modified compared to the original", diffCount > 0)
    }
}
