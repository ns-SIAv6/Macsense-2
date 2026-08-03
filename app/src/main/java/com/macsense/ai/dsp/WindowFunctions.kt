package com.macsense.ai.dsp

import kotlin.math.PI
import kotlin.math.cos

object WindowFunctions {
    fun hann(n: Int): DoubleArray {
        val w = DoubleArray(n)
        for (i in 0 until n) {
            w[i] = 0.5 * (1.0 - cos(2.0 * PI * i / (n - 1)))
        }
        return w
    }

    fun hamming(n: Int): DoubleArray {
        val w = DoubleArray(n)
        for (i in 0 until n) {
            w[i] = 0.54 - 0.46 * cos(2.0 * PI * i / (n - 1))
        }
        return w
    }

    fun blackmanHarris(n: Int): DoubleArray {
        val w = DoubleArray(n)
        val a0 = 0.35875
        val a1 = 0.48829
        val a2 = 0.14128
        val a3 = 0.01168
        for (i in 0 until n) {
            val ratio = 2.0 * PI * i / (n - 1)
            w[i] = a0 - a1 * cos(ratio) + a2 * cos(2.0 * ratio) - a3 * cos(3.0 * ratio)
        }
        return w
    }
}
