package com.macsense.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SectionInfo(val id: String, val name: String)

class DawViewModel : ViewModel() {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _barPosition = MutableStateFlow(1)
    val barPosition: StateFlow<Int> = _barPosition.asStateFlow()
    
    private val _sections = MutableStateFlow(listOf(
        SectionInfo("intro", "Intro"),
        SectionInfo("verse1", "Verse 1"),
        SectionInfo("hook", "Hook")
    ))
    val sections: StateFlow<List<SectionInfo>> = _sections.asStateFlow()
    
    private var playbackJob: Job? = null
    private val tempoBpm = 120.0
    
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
    
    fun reorderSection(fromIndex: Int, toIndex: Int) {
        val currentList = _sections.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _sections.value = currentList
        }
    }
    
    fun advanceBar() {
        _barPosition.value += 1
    }
    
    private fun startTransportClock() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val barDurationMs = (240000.0 / tempoBpm).toLong() // 4 beats per bar
            while (true) {
                delay(barDurationMs)
                advanceBar()
            }
        }
    }
}
