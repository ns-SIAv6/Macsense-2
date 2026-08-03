package com.macsense.ai.audio

class TransportClock {
    var isPlaying = false
    var bpm = 120.0
    
    fun start() {
        isPlaying = true
    }
    
    fun stop() {
        isPlaying = false
    }
}
