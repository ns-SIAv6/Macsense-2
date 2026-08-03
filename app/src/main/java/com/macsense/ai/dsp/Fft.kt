package com.macsense.ai.dsp

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.sqrt

object Fft {
    fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        if (n == 0) return
        require((n and (n - 1)) == 0) { "FFT size must be a power of 2" }
        require(re.size == im.size) { "Real and imaginary arrays must be of same size" }

        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempRe = re[i]
                val tempIm = im[i]
                re[i] = re[j]
                im[i] = im[j]
                re[j] = tempRe
                im[j] = tempIm
            }
            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }

        var l = 1
        while (l < n) {
            val step = 2 * l
            val angle = -PI / l
            var wRe = 1.0
            var wIm = 0.0
            val wMultRe = cos(angle)
            val wMultIm = sin(angle)
            for (m in 0 until l) {
                var i = m
                while (i < n) {
                    val k = i + l
                    val tempRe = wRe * re[k] - wIm * im[k]
                    val tempIm = wRe * im[k] + wIm * re[k]
                    re[k] = re[i] - tempRe
                    im[k] = im[i] - tempIm
                    re[i] += tempRe
                    im[i] += tempIm
                    i += step
                }
                val nextWRe = wRe * wMultRe - wIm * wMultIm
                val nextWIm = wRe * wMultIm + wIm * wMultRe
                wRe = nextWRe
                wIm = nextWIm
            }
            l = step
        }
    }

    fun inverseFft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        for (i in 0 until n) {
            im[i] = -im[i]
        }
        fft(re, im)
        for (i in 0 until n) {
            re[i] /= n.toDouble()
            im[i] = -im[i] / n.toDouble()
        }
    }

    fun magnitudeSpectrum(samples: DoubleArray, window: DoubleArray): DoubleArray {
        val n = samples.size
        val re = DoubleArray(n)
        val im = DoubleArray(n)
        for (i in 0 until n) {
            re[i] = samples[i] * window[i]
        }
        fft(re, im)
        val mag = DoubleArray(n / 2 + 1)
        for (i in mag.indices) {
            mag[i] = sqrt(re[i] * re[i] + im[i] * im[i])
        }
        return mag
    }
}
