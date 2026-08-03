package com.macsense.ai.dsp

import org.junit.Test
import org.junit.Assert.assertEquals

class FftTest {
    @Test
    fun testRealForward() {
        val n = 4
        val real = doubleArrayOf(1.0, 0.0, 0.0, 0.0)
        val imag = DoubleArray(4)
        
        Fft.fft(real, imag)

        // Impulse response forward FFT results in a flat magnitude of 1.0 across all frequency bins, and zero phase.
        for (i in 0 until n) {
            assertEquals(1.0, real[i], 0.001)
            assertEquals(0.0, imag[i], 0.001)
        }
    }
}
