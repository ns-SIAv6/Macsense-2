package com.macsense.ai.dsp

object OnsetDetector {
    fun detectOnsets(samples: DoubleArray, sampleRate: Int): List<Double> {
        val hopSize = 512
        val windowSize = 1024
        val onsets = mutableListOf<Double>()
        if (samples.size < windowSize) return onsets

        var prevMag = DoubleArray(windowSize / 2 + 1)
        val window = WindowFunctions.hann(windowSize)
        val flux = DoubleArray(samples.size / hopSize)

        for (i in 0 until samples.size - windowSize step hopSize) {
            val frame = DoubleArray(windowSize)
            System.arraycopy(samples, i, frame, 0, windowSize)
            val mag = Fft.magnitudeSpectrum(frame, window)
            
            var diff = 0.0
            for (j in mag.indices) {
                val d = mag[j] - prevMag[j]
                if (d > 0.0) {
                    diff += d
                }
            }
            flux[i / hopSize] = diff
            prevMag = mag
        }

        val windowLen = 5
        val thresholdMultiplier = 1.2
        for (i in windowLen until flux.size - windowLen) {
            val localWindow = flux.sliceArray(i - windowLen until i + windowLen)
            localWindow.sort()
            val median = localWindow[localWindow.size / 2]
            val threshold = median * thresholdMultiplier + 0.05
            
            if (flux[i] > threshold && flux[i] > flux[i - 1] && flux[i] > flux[i + 1]) {
                val time = i * hopSize.toDouble() / sampleRate
                if (onsets.isEmpty() || time - onsets.last() > 0.05) {
                    onsets.add(time)
                }
            }
        }
        return onsets
    }
}
