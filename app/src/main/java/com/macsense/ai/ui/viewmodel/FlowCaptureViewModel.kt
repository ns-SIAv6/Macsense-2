package com.macsense.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class RecordSession(
    val id: String,
    val durationSeconds: Double,
    val autoBpm: Double,
    val cadenceStyle: String,
    val performanceStyle: String,
    val quantizeFeel: String,
    val isAligned: Boolean
)

class FlowCaptureViewModel : ViewModel() {
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _elapsedSeconds = MutableStateFlow(0.0)
    val elapsedSeconds: StateFlow<Double> = _elapsedSeconds.asStateFlow()
    
    private val _autoBpm = MutableStateFlow(0.0)
    val autoBpm: StateFlow<Double> = _autoBpm.asStateFlow()
    
    private val _cadenceStyle = MutableStateFlow("Melodic Trap")
    val cadenceStyle: StateFlow<String> = _cadenceStyle.asStateFlow()
    
    private val _quantizeFeel = MutableStateFlow("Groove Swing (16th)")
    val quantizeFeel: StateFlow<String> = _quantizeFeel.asStateFlow()
    
    private val _performanceStyle = MutableStateFlow("Laidback")
    val performanceStyle: StateFlow<String> = _performanceStyle.asStateFlow()
    
    private val _autoAlignEnabled = MutableStateFlow(true)
    val autoAlignEnabled: StateFlow<Boolean> = _autoAlignEnabled.asStateFlow()
    
    private val _recordedTakes = MutableStateFlow<List<RecordSession>>(emptyList())
    val recordedTakes: StateFlow<List<RecordSession>> = _recordedTakes.asStateFlow()
    
    private var recordJob: Job? = null
    
    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }
    
    fun startRecording() {
        _isRecording.value = true
        _elapsedSeconds.value = 0.0
        _autoBpm.value = 0.0
        
        recordJob?.cancel()
        recordJob = viewModelScope.launch(Dispatchers.Default) {
            var counter = 0
            while (_isRecording.value) {
                delay(100)
                _elapsedSeconds.value += 0.1
                counter++
                
                // Simulate periodic auto BPM detection
                if (counter % 30 == 0 && counter > 0) {
                    _autoBpm.value = Random.nextDouble(118.0, 126.0)
                }
            }
        }
    }
    
    fun stopRecording() {
        _isRecording.value = false
        recordJob?.cancel()
        recordJob = null
        
        val duration = _elapsedSeconds.value
        if (duration > 1.0) {
            val detectedBpm = if (_autoBpm.value == 0.0) 120.0 else _autoBpm.value
            val take = RecordSession(
                id = "take_${System.currentTimeMillis()}",
                durationSeconds = duration,
                autoBpm = detectedBpm,
                cadenceStyle = _cadenceStyle.value,
                performanceStyle = _performanceStyle.value,
                quantizeFeel = _quantizeFeel.value,
                isAligned = _autoAlignEnabled.value
            )
            _recordedTakes.value = _recordedTakes.value + take
        }
    }
    
    fun setCadenceStyle(style: String) {
        _cadenceStyle.value = style
    }
    
    fun setQuantizeFeel(feel: String) {
        _quantizeFeel.value = feel
    }
    
    fun setPerformanceStyle(style: String) {
        _performanceStyle.value = style
    }
    
    fun setAutoAlignEnabled(enabled: Boolean) {
        _autoAlignEnabled.value = enabled
    }
    
    fun deleteTake(id: String) {
        _recordedTakes.value = _recordedTakes.value.filter { it.id != id }
    }
    
    override fun onCleared() {
        super.onCleared()
        recordJob?.cancel()
    }
}
