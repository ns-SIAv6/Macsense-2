package com.macsense.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macsense.ai.dsp.LoudnessMeter
import com.macsense.ai.dsp.TruePeakMeter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

data class MasteringPreset(
    val id: String,
    val name: String,
    val targetLufs: Double,
    val ceilingDbtp: Double,
    val character: String // "Clean", "Warm", "Punchy", "Aggressive"
)

class MasteringViewModel : ViewModel() {
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _targetLufs = MutableStateFlow(-14.0)
    val targetLufs: StateFlow<Double> = _targetLufs.asStateFlow()
    
    private val _ceilingDbtp = MutableStateFlow(-1.0)
    val ceilingDbtp: StateFlow<Double> = _ceilingDbtp.asStateFlow()
    
    private val _currentLufs = MutableStateFlow(-60.0)
    val currentLufs: StateFlow<Double> = _currentLufs.asStateFlow()
    
    private val _currentDbtp = MutableStateFlow(-60.0)
    val currentDbtp: StateFlow<Double> = _currentDbtp.asStateFlow()
    
    private val _eqLowGain = MutableStateFlow(0.0f)
    val eqLowGain: StateFlow<Float> = _eqLowGain.asStateFlow()
    
    private val _eqMidGain = MutableStateFlow(0.0f)
    val eqMidGain: StateFlow<Float> = _eqMidGain.asStateFlow()
    
    private val _eqHighGain = MutableStateFlow(0.0f)
    val eqHighGain: StateFlow<Float> = _eqHighGain.asStateFlow()
    
    private val _limiterThreshold = MutableStateFlow(-3.0f)
    val limiterThreshold: StateFlow<Float> = _limiterThreshold.asStateFlow()
    
    private val _compressorThreshold = MutableStateFlow(-12.0f)
    val compressorThreshold: StateFlow<Float> = _compressorThreshold.asStateFlow()
    
    private val _presets = MutableStateFlow(listOf(
        MasteringPreset("streaming", "Streaming Standard", -14.0, -1.0, "Clean"),
        MasteringPreset("club", "Club Heavy", -9.0, -0.5, "Warm"),
        MasteringPreset("radio", "FM Radio Peak", -11.0, -1.0, "Punchy"),
        MasteringPreset("aggressive", "Maximum Loudness", -6.0, -0.2, "Aggressive")
    ))
    val presets: StateFlow<List<MasteringPreset>> = _presets.asStateFlow()
    
    private val _selectedPresetId = MutableStateFlow("streaming")
    val selectedPresetId: StateFlow<String> = _selectedPresetId.asStateFlow()
    
    private var dspJob: Job? = null
    
    fun setTargetLufs(lufs: Double) {
        _targetLufs.value = lufs.coerceIn(-24.0, -4.0)
    }
    
    fun setCeilingDbtp(dbtp: Double) {
        _ceilingDbtp.value = dbtp.coerceIn(-6.0, 0.0)
    }
    
    fun setEqGains(low: Float, mid: Float, high: Float) {
        _eqLowGain.value = low.coerceIn(-12f, 12f)
        _eqMidGain.value = mid.coerceIn(-12f, 12f)
        _eqHighGain.value = high.coerceIn(-12f, 12f)
    }
    
    fun setLimiterThreshold(threshold: Float) {
        _limiterThreshold.value = threshold.coerceIn(-24f, 0f)
    }
    
    fun setCompressorThreshold(threshold: Float) {
        _compressorThreshold.value = threshold.coerceIn(-48f, 0f)
    }
    
    fun applyPreset(presetId: String) {
        _presets.value.find { it.id == presetId }?.let { preset ->
            _selectedPresetId.value = presetId
            _targetLufs.value = preset.targetLufs
            _ceilingDbtp.value = preset.ceilingDbtp
            when (preset.character) {
                "Clean" -> setEqGains(0f, 0f, 1f)
                "Warm" -> setEqGains(2f, 1f, -1f)
                "Punchy" -> setEqGains(1.5f, -1f, 1.5f)
                "Aggressive" -> setEqGains(3f, 2f, 3f)
            }
        }
    }
    
    fun startMasteringProcess() {
        if (_isProcessing.value) return
        _isProcessing.value = true
        
        dspJob?.cancel()
        dspJob = viewModelScope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val size = 44100 // 1 second buffer
            val bufferLeft = DoubleArray(size)
            val bufferRight = DoubleArray(size)
            val stereoBuffer = arrayOf(bufferLeft, bufferRight)
            
            var phase = 0.0
            while (_isProcessing.value) {
                // Generate simulated pre-master mix
                for (i in 0 until size) {
                    val base = sin(phase) * 0.4 + sin(phase * 3.0) * 0.15
                    // Apply Mastering gain based on Target LUFS vs Thresholds
                    val gainFactor = if (_targetLufs.value > -14.0) 2.2 else 1.2
                    bufferLeft[i] = (base * gainFactor).coerceIn(-1.0, 1.0)
                    bufferRight[i] = (base * gainFactor * 0.95).coerceIn(-1.0, 1.0)
                    
                    phase += (2.0 * Math.PI * 440.0 / sampleRate)
                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                }
                
                // Measure real-time Loudness (LUFS)
                val measuredLufs = LoudnessMeter.integratedLufs(stereoBuffer, sampleRate)
                _currentLufs.value = if (measuredLufs.isInfinite() || measuredLufs.isNaN()) -60.0 else measuredLufs
                
                // Measure real-time True Peak (dBTP)
                val measuredDbtp = TruePeakMeter.measureDbtp(bufferLeft)
                _currentDbtp.value = if (measuredDbtp.isInfinite() || measuredDbtp.isNaN()) -60.0 else measuredDbtp
                
                delay(1000) // update stats once per second
            }
        }
    }
    
    fun stopMasteringProcess() {
        _isProcessing.value = false
        dspJob?.cancel()
        dspJob = null
        _currentLufs.value = -60.0
        _currentDbtp.value = -60.0
    }
    
    override fun onCleared() {
        super.onCleared()
        dspJob?.cancel()
    }
}
