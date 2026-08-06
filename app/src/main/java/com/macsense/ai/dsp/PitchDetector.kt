package com.macsense.ai.dsp

class PitchResult(val frequency: Double, val confidence: Double)

object PitchDetector {

    private const val THRESHOLD = 0.1

    fun detect(
        samples: DoubleArray,
        sampleRate: Int,
        minFreq: Double = 50.0,
        maxFreq: Double = 2000.0
    ): PitchResult? {
        if (samples.isEmpty() || sampleRate <= 0 || minFreq <= 0.0 || maxFreq <= 0.0 || minFreq >= maxFreq) {
            return null
        }
        val minTau = (sampleRate / maxFreq).toInt().coerceAtLeast(2)
        val maxTau = (sampleRate / minFreq).toInt()
        val window = minOf(samples.size, maxTau * 4)
        if (window < maxTau * 2) return null

        val halfW = window / 2
        val tauMax = minOf(maxTau, halfW - 1)
        if (tauMax <= minTau) return null

        // Step 1: difference function
        val d = DoubleArray(tauMax + 1)
        for (tau in 1..tauMax) {
            var sum = 0.0
            for (i in 0 until halfW) {
                val delta = samples[i] - samples[i + tau]
                sum += delta * delta
            }
            d[tau] = sum
        }

        // Step 2: cumulative mean normalized difference
        val dPrime = DoubleArray(tauMax + 1)
        dPrime[0] = 1.0
        var running = 0.0
        for (tau in 1..tauMax) {
            running += d[tau]
            dPrime[tau] = if (running == 0.0) 1.0 else d[tau] * tau / running
        }

        // Step 3: absolute threshold — first dip below THRESHOLD, then descend
        var tauEstimate = -1
        var tau = minTau
        while (tau <= tauMax) {
            if (dPrime[tau] < THRESHOLD) {
                while (tau + 1 <= tauMax && dPrime[tau + 1] < dPrime[tau]) tau++
                tauEstimate = tau
                break
            }
            tau++
        }
        if (tauEstimate == -1) {
            var best = minTau
            for (t in minTau..tauMax) if (dPrime[t] < dPrime[best]) best = t
            if (dPrime[best] > 0.5) return null
            tauEstimate = best
        }

        // Step 4: parabolic interpolation around tauEstimate
        val t0 = tauEstimate
        val refined: Double = if (t0 > 1 && t0 < tauMax) {
            val s0 = dPrime[t0 - 1]
            val s1 = dPrime[t0]
            val s2 = dPrime[t0 + 1]
            val denom = 2.0 * (2.0 * s1 - s2 - s0)
            if (denom == 0.0) t0.toDouble() else t0 + (s2 - s0) / denom
        } else t0.toDouble()

        if (refined <= 0.0) return null
        val freq = sampleRate / refined
        val confidence = (1.0 - dPrime[t0]).coerceIn(0.0, 1.0)
        return PitchResult(freq, confidence)
    }
}
