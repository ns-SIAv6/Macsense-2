package com.macsense.ai.audio

import com.macsense.ai.dsp.Fft
import com.macsense.ai.dsp.TruePeakMeter
import kotlin.math.log10
import kotlin.math.max

/**
 * Drives the DAW's live meters and spectrum analyzer from genuine microphone PCM,
 * replacing the previous synthesized sine+noise placeholder signal.
 *
 * Owns an [AudioCapture] instance and analyzes audio directly from the capture thread's
 * chunk callback into a fixed-size ring buffer, so the FFT/true-peak frame is always the
 * most recent N samples regardless of the UI's polling cadence.
 */
class LiveMeterEngine(
    private val sampleRate: Int = AudioCapture.DEFAULT_SAMPLE_RATE,
    private val fftSize: Int = DEFAULT_FFT_SIZE,
    private val spectrumBands: Int = DEFAULT_SPECTRUM_BANDS
) {
    private val capture = AudioCapture(sampleRate)
    private val ring = DoubleArray(RING_CAPACITY)
    private var writeIndex = 0
    private var filled = 0

    @Volatile var latestSpectrumDb: FloatArray = FloatArray(spectrumBands) { SILENCE_DB }
        private set
    @Volatile var latestPeakDbL: Float = SILENCE_DB
        private set
    @Volatile var latestPeakDbR: Float = SILENCE_DB
        private set
    @Volatile var latestTruePeakDb: Double = Double.NEGATIVE_INFINITY
        private set

    /** Starts microphone capture and live analysis. Returns false if mic init failed. */
    fun start(): Boolean {
        writeIndex = 0
        filled = 0
        return capture.start(onChunk = ::ingest)
    }

    /** Feeds a chunk of freshly captured PCM into the ring buffer and recomputes analysis. */
    @Synchronized
    fun ingest(samples: DoubleArray) {
        for (s in samples) {
            ring[writeIndex] = s
            writeIndex = (writeIndex + 1) % ring.size
            if (filled < ring.size) filled++
        }
        analyze()
    }

    private fun analyze() {
        val n = fftSize
        if (filled < n) return
        val frame = DoubleArray(n)
        val start = (writeIndex - n + ring.size) % ring.size
        for (i in 0 until n) {
            frame[i] = ring[(start + i) % ring.size]
        }

        val re = frame.copyOf()
        val im = DoubleArray(n)
        Fft.fft(re, im)

        val bands = FloatArray(spectrumBands)
        val binsPerBand = max(1, (n / 2) / spectrumBands)
        for (band in 0 until spectrumBands) {
            var sum = 0.0
            var count = 0
            val startBin = band * binsPerBand
            val endBin = min(startBin + binsPerBand, n / 2)
            for (bin in startBin until endBin) {
                sum += Math.sqrt(re[bin] * re[bin] + im[bin] * im[bin])
                count++
            }
            val mag = if (count > 0) sum / count else 0.0
            val db = (20.0 * log10(mag + 1e-9)).toFloat()
            bands[band] = db.coerceIn(-80f, 0f)
        }
        latestSpectrumDb = bands

        var peak = 0.0
        for (s in frame) if (Math.abs(s) > peak) peak = Math.abs(s)
        val peakDb = (20.0 * log10(peak + 1e-9)).toFloat().coerceIn(-60f, 0f)
        latestPeakDbL = peakDb
        latestPeakDbR = peakDb
        latestTruePeakDb = TruePeakMeter.measureDbtp(frame)
    }

    /** Stops capture, resetting meters to silence. */
    fun stop() {
        capture.stop()
        latestSpectrumDb = FloatArray(spectrumBands) { SILENCE_DB }
        latestPeakDbL = SILENCE_DB
        latestPeakDbR = SILENCE_DB
        latestTruePeakDb = Double.NEGATIVE_INFINITY
    }

    private fun min(a: Int, b: Int) = if (a < b) a else b

    companion object {
        const val DEFAULT_FFT_SIZE = 1024
        const val DEFAULT_SPECTRUM_BANDS = 32
        private const val RING_CAPACITY = 8192
        private const val SILENCE_DB = -80f
    }
}
