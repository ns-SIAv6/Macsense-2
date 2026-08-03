package com.macsense.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macsense.ai.dsp.Fft
import com.macsense.ai.dsp.LoudnessMeter
import com.macsense.ai.dsp.TruePeakMeter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

data class SectionInfo(
    val id: String,
    val name: String,
    val barCount: Int = 8,
    val isExpanded: Boolean = false,
    val lyrics: String = "Yeah, double cup spilling on the MPC\nBeat so hard, MACSENSE setting me free",
    val instrumentGrid: Map<String, List<Boolean>> = createDefaultGrid(),
    val reverb: Float = 0.25f,
    val delay: Float = 0.15f,
    val filter: Float = 0.85f,
    val volume: Float = 0.75f
)

fun createDefaultGrid(): Map<String, List<Boolean>> {
    val lanes = listOf(
        "808/Bass", "Kick", "Snare", "Hi-Hat", "Clap", "Percussion",
        "Riser", "Crash", "Bass Synth", "Lead", "Pads", "Vocal/Adlib"
    )
    return lanes.associateWith { lane ->
        List(16) { index ->
            when (lane) {
                "Kick" -> index % 4 == 0
                "Snare" -> index % 8 == 4
                "Hi-Hat" -> index % 2 == 0
                "808/Bass" -> index == 0 || index == 10
                else -> false
            }
        }
    }
}

class DawViewModel : ViewModel() {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _barPosition = MutableStateFlow(1)
    val barPosition: StateFlow<Int> = _barPosition.asStateFlow()
    
    private val _sections = MutableStateFlow(listOf(
        SectionInfo("intro", "Intro", barCount = 4),
        SectionInfo("verse1", "Verse 1", barCount = 16),
        SectionInfo("hook", "Hook", barCount = 8),
        SectionInfo("bridge", "Bridge", barCount = 8),
        SectionInfo("outro", "Outro", barCount = 4)
    ))
    val sections: StateFlow<List<SectionInfo>> = _sections.asStateFlow()
    
    private val _bpm = MutableStateFlow(120.0)
    val bpm: StateFlow<Double> = _bpm.asStateFlow()
    
    private val _timecode = MutableStateFlow("00:00:00")
    val timecode: StateFlow<String> = _timecode.asStateFlow()
    
    private val _meterL = MutableStateFlow(-60.0f)
    val meterL: StateFlow<Float> = _meterL.asStateFlow()
    
    private val _meterR = MutableStateFlow(-60.0f)
    val meterR: StateFlow<Float> = _meterR.asStateFlow()
    
    private val _spectrumData = MutableStateFlow(FloatArray(32) { -80f })
    val spectrumData: StateFlow<FloatArray> = _spectrumData.asStateFlow()
    
    private var playbackJob: Job? = null
    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    
    init {
        startSimulationLoop()
    }
    
    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }
    
    fun play() {
        _isPlaying.value = true
        startTransportClock()
    }
    
    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }
    
    fun advanceBar() {
        _barPosition.value += 1
    }
    
    fun updateBpm(newBpm: Double) {
        if (newBpm in 40.0..250.0) {
            _bpm.value = newBpm
            if (_isPlaying.value) {
                startTransportClock() // Restart to apply new duration
            }
        }
    }
    
    fun reorderSection(fromIndex: Int, toIndex: Int) {
        val currentList = _sections.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _sections.value = currentList
        }
    }
    
    fun toggleSectionExpanded(id: String) {
        _sections.value = _sections.value.map {
            if (it.id == id) it.copy(isExpanded = !it.isExpanded) else it
        }
    }
    
    fun updateSectionLyrics(id: String, newLyrics: String) {
        _sections.value = _sections.value.map {
            if (it.id == id) it.copy(lyrics = newLyrics) else it
        }
    }
    
    fun updateInstrumentStep(sectionId: String, lane: String, stepIndex: Int, value: Boolean) {
        _sections.value = _sections.value.map { section ->
            if (section.id == sectionId) {
                val newGrid = section.instrumentGrid.toMutableMap()
                val currentSteps = newGrid[lane]?.toMutableList() ?: MutableList(16) { false }
                if (stepIndex in currentSteps.indices) {
                    currentSteps[stepIndex] = value
                    newGrid[lane] = currentSteps
                }
                section.copy(instrumentGrid = newGrid)
            } else section
        }
    }
    
    fun updateSectionReverb(sectionId: String, value: Float) {
        _sections.value = _sections.value.map {
            if (it.id == sectionId) it.copy(reverb = value) else it
        }
    }

    fun updateSectionDelay(sectionId: String, value: Float) {
        _sections.value = _sections.value.map {
            if (it.id == sectionId) it.copy(delay = value) else it
        }
    }

    fun updateSectionFilter(sectionId: String, value: Float) {
        _sections.value = _sections.value.map {
            if (it.id == sectionId) it.copy(filter = value) else it
        }
    }

    fun updateSectionVolume(sectionId: String, value: Float) {
        _sections.value = _sections.value.map {
            if (it.id == sectionId) it.copy(volume = value) else it
        }
    }
    
    private fun startTransportClock() {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (true) {
                val barDurationMs = (240000.0 / _bpm.value).toLong()
                delay(barDurationMs)
                advanceBar()
            }
        }
    }
    
    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val bufferSize = 512
            val re = DoubleArray(bufferSize)
            val im = DoubleArray(bufferSize)
            var phase = 0.0
            
            while (true) {
                if (_isPlaying.value) {
                    // Update timecode
                    val totalBeats = (_barPosition.value - 1) * 4.0
                    val totalSeconds = (totalBeats / _bpm.value) * 60.0
                    val minutes = (totalSeconds / 60).toInt()
                    val seconds = (totalSeconds % 60).toInt()
                    val ms = ((totalSeconds % 1.0) * 100).toInt()
                    _timecode.value = String.format("%02d:%02d:%02d", minutes, seconds, ms)
                    
                    // Generate realistic synthesized signal for meters and FFT spectrum
                    for (i in 0 until bufferSize) {
                        val sineSignal = sin(phase) * 0.5 + sin(phase * 2.5) * 0.2 + cos(phase * 0.5) * 0.15
                        re[i] = sineSignal + Random.nextDouble(-0.05, 0.05) // Add noise
                        im[i] = 0.0
                        phase += (2.0 * Math.PI * 220.0 / sampleRate) // 220Hz fundamental
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                    }
                    
                    // Run FFT
                    Fft.fft(re, im)
                    val spec = FloatArray(32)
                    for (i in 0 until 32) {
                        val mag = Math.sqrt(re[i] * re[i] + im[i] * im[i])
                        val db = (20.0 * Math.log10(mag + 1e-5)).toFloat()
                        spec[i] = db.coerceIn(-80f, 0f)
                    }
                    _spectrumData.value = spec
                    
                    // Calculate Meter levels (peaks)
                    val peakValL = re.maxOrNull() ?: 0.0
                    val peakValR = re.minOrNull() ?: 0.0
                    val dbL = (20.0 * Math.log10(Math.abs(peakValL) + 1e-5)).toFloat()
                    val dbR = (20.0 * Math.log10(Math.abs(peakValR) + 1e-5)).toFloat()
                    _meterL.value = dbL.coerceIn(-60f, 0f)
                    _meterR.value = dbR.coerceIn(-60f, 0f)
                } else {
                    _meterL.value = -60.0f
                    _meterR.value = -60.0f
                    val spec = _spectrumData.value.clone()
                    for (i in spec.indices) {
                        spec[i] = (spec[i] - 1.5f).coerceAtLeast(-80f) // Decaying spectrum
                    }
                    _spectrumData.value = spec
                }
                delay(50) // 20 FPS updates
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        simulationJob?.cancel()
    }
}
