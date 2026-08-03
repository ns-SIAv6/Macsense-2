package com.example.audio

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow

/**
 * Real-time MIDI Controller Interface Layer for MA¢SENSE.
 * Handles physical USB/Bluetooth MIDI devices, CC Parameter Mapping to SoundBreeder traits,
 * pitch bend, and QWERTY/onscreen virtual keyboard inputs.
 */
class MidiControllerManager(private val context: Context? = null) {

    data class MidiState(
        val isConnected: Boolean = true,
        val deviceName: String = "MA¢SENSE Virtual Controller (USB/BT)",
        val lastNotePressed: Int? = null,
        val lastNoteName: String = "",
        val lastVelocity: Int = 0,
        val lastCcNumber: Int? = null,
        val lastCcValue: Int? = null,
        val activeCcTarget: String = "",
        val paramMassOverride: Float? = null,
        val paramRadianceOverride: Float? = null,
        val paramEntropyOverride: Float? = null,
        val paramCurvatureOverride: Float? = null,
        val paramBreedWeightOverride: Float? = null,
        val paramMutationFactorOverride: Float? = null,
        val paramChrom1FreqOverride: Int? = null,
        val paramChrom5DistortionOverride: Int? = null
    )

    private val _midiState = MutableStateFlow(MidiState())
    val midiState: StateFlow<MidiState> = _midiState.asStateFlow()

    private var midiManager: MidiManager? = null

    init {
        try {
            context?.let {
                midiManager = it.getSystemService(Context.MIDI_SERVICE) as? MidiManager
                scanAndConnectMidiDevices()
            }
        } catch (_: Exception) {}
    }

    private fun scanAndConnectMidiDevices() {
        val manager = midiManager ?: return
        val devices = manager.devices
        if (devices.isNotEmpty()) {
            val deviceInfo = devices.first()
            val name = deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "USB MIDI Keyboard"
            manager.openDevice(deviceInfo, { device ->
                if (device != null) {
                    val outputPort = device.openOutputPort(0)
                    outputPort?.connect(MidiDataReceiver())
                    _midiState.value = _midiState.value.copy(
                        isConnected = true,
                        deviceName = name
                    )
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    /**
     * Process raw MIDI Control Change (CC) messages and map them directly to SoundBreeder parameters.
     */
    fun processMidiCc(ccNumber: Int, value: Int, channel: Int = 1) {
        val normalized = (value.coerceIn(0, 127) / 127.0f)
        val currentState = _midiState.value

        val (targetName, updatedState) = when (ccNumber) {
            1 -> "Radiance" to currentState.copy(
                paramRadianceOverride = normalized,
                lastCcNumber = ccNumber,
                lastCcValue = value
            )
            7 -> "Breed Weight" to currentState.copy(
                paramBreedWeightOverride = (0.1f + normalized * 0.8f),
                lastCcNumber = ccNumber,
                lastCcValue = value
            )
            16 -> "Mass" to currentState.copy(
                paramMassOverride = (0.05f + normalized * 0.95f),
                lastCcNumber = ccNumber,
                lastCcValue = value
            )
            17 -> "Mutation Factor" to currentState.copy(
                paramMutationFactorOverride = (0.01f + normalized * 0.34f),
                lastCcNumber = ccNumber,
                lastCcValue = value
            )
            18 -> "Base Frequency" to currentState.copy(
                paramChrom1FreqOverride = (20 + (normalized.pow(2) * 1980)).toInt(),
                lastCcNumber = ccNumber,
                lastCcValue = value
            )
            19 -> "Distortion" to currentState.copy(
                paramChrom5DistortionOverride = (normalized * 100).toInt(),
                lastCcNumber = ccNumber,
                lastCcValue = value
            )
            71 -> "Entropy" to currentState.copy(
                paramEntropyOverride = normalized,
                lastCcNumber = ccNumber,
                lastCcValue = value
            )
            74 -> "Curvature" to currentState.copy(
                paramCurvatureOverride = normalized,
                lastCcNumber = ccNumber,
                lastCcValue = value
            )
            else -> "CC #$ccNumber" to currentState.copy(
                lastCcNumber = ccNumber,
                lastCcValue = value
            )
        }

        _midiState.value = updatedState.copy(activeCcTarget = targetName)
    }

    /**
     * Process MIDI Note On event
     */
    fun processNoteOn(note: Int, velocity: Int) {
        val noteName = getNoteName(note)
        _midiState.value = _midiState.value.copy(
            lastNotePressed = note,
            lastNoteName = noteName,
            lastVelocity = velocity
        )
    }

    /**
     * Process MIDI Note Off event
     */
    fun processNoteOff(note: Int) {
        if (_midiState.value.lastNotePressed == note) {
            _midiState.value = _midiState.value.copy(
                lastVelocity = 0
            )
        }
    }

    /**
     * Map QWERTY/Hardware Keyboard key events directly to MIDI Notes.
     */
    fun processKeyEvent(keyCode: Int, isDown: Boolean): Boolean {
        val note = when (keyCode) {
            KeyEvent.KEYCODE_A -> 60  // C4
            KeyEvent.KEYCODE_W -> 61  // C#4
            KeyEvent.KEYCODE_S -> 62  // D4
            KeyEvent.KEYCODE_E -> 63  // D#4
            KeyEvent.KEYCODE_D -> 64  // E4
            KeyEvent.KEYCODE_F -> 65  // F4
            KeyEvent.KEYCODE_T -> 66  // F#4
            KeyEvent.KEYCODE_G -> 67  // G4
            KeyEvent.KEYCODE_Y -> 68  // G#4
            KeyEvent.KEYCODE_H -> 69  // A4
            KeyEvent.KEYCODE_U -> 70  // A#4
            KeyEvent.KEYCODE_J -> 71  // B4
            KeyEvent.KEYCODE_K -> 72  // C5
            KeyEvent.KEYCODE_O -> 73  // C#5
            KeyEvent.KEYCODE_L -> 74  // D5
            else -> null
        }

        if (note != null) {
            if (isDown) {
                processNoteOn(note, 100)
            } else {
                processNoteOff(note)
            }
            return true
        }
        return false
    }

    /**
     * Set a virtual CC value normalized 0.0 to 1.0 from UI controls.
     */
    fun setVirtualCc(ccNumber: Int, normalizedValue: Float) {
        val ccValue = (normalizedValue.coerceIn(0f, 1f) * 127).toInt()
        processMidiCc(ccNumber, ccValue)
    }

    /**
     * Convert MIDI note number (0..127) to musical note string (e.g. 60 -> "C4")
     */
    fun getNoteName(note: Int): String {
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val octave = (note / 12) - 1
        val noteIdx = note % 12
        return "${noteNames[noteIdx]}$octave"
    }

    fun getNoteFrequency(note: Int): Float {
        return (440.0 * 2.0.pow((note - 69) / 12.0)).toFloat()
    }

    private inner class MidiDataReceiver : MidiReceiver() {
        override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
            if (count >= 3) {
                val status = msg[offset].toInt() and 0xFF
                val command = status and 0xF0
                val data1 = msg[offset + 1].toInt() and 0x7F
                val data2 = msg[offset + 2].toInt() and 0x7F

                when (command) {
                    0x90 -> { // Note On
                        if (data2 > 0) processNoteOn(data1, data2) else processNoteOff(data1)
                    }
                    0x80 -> { // Note Off
                        processNoteOff(data1)
                    }
                    0xB0 -> { // Control Change (CC)
                        processMidiCc(data1, data2)
                    }
                }
            }
        }
    }
}
