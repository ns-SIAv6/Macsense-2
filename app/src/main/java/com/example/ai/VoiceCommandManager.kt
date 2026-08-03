package com.example.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Real-Time Voice-to-Text Speech Recognition Engine for MA¢SENSE Studio.
 * Captures microphone audio input and transcribes natural language voice commands for ARi (Gemini API).
 */
class VoiceCommandManager(private val context: Context) {

    data class VoiceState(
        val isListening: Boolean = false,
        val isProcessing: Boolean = false,
        val lastSpokenText: String = "",
        val lastExecutedAction: String = "",
        val errorMessage: String? = null,
        val partialText: String = ""
    )

    private val _voiceState = MutableStateFlow(VoiceState())
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = _voiceState.value.copy(
                            isListening = true,
                            isProcessing = false,
                            errorMessage = null,
                            partialText = "Listening for Ari command..."
                        )
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceState.value = _voiceState.value.copy(
                            partialText = "Hearing voice..."
                        )
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _voiceState.value = _voiceState.value.copy(
                            isListening = false,
                            isProcessing = true,
                            partialText = "Processing speech with Ari..."
                        )
                    }

                    override fun onError(error: Int) {
                        val errorDesc = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timed out"
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            else -> "Voice recognition error ($error)"
                        }
                        _voiceState.value = _voiceState.value.copy(
                            isListening = false,
                            isProcessing = false,
                            errorMessage = errorDesc,
                            partialText = ""
                        )
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _voiceState.value = _voiceState.value.copy(
                            isListening = false,
                            isProcessing = false,
                            lastSpokenText = text,
                            partialText = ""
                        )
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            _voiceState.value = _voiceState.value.copy(
                                partialText = text
                            )
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else {
            _voiceState.value = _voiceState.value.copy(
                errorMessage = "Speech recognition not supported on this device"
            )
        }
    }

    /**
     * Start listening to user voice command via microphone
     */
    fun startListening() {
        val recognizer = speechRecognizer ?: run {
            initSpeechRecognizer()
            speechRecognizer
        }

        if (recognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to ARi Co-Producer...")
            }
            try {
                recognizer.startListening(intent)
                _voiceState.value = _voiceState.value.copy(
                    isListening = true,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _voiceState.value = _voiceState.value.copy(
                    isListening = false,
                    errorMessage = "Error starting microphone: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Stop listening manually
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _voiceState.value = _voiceState.value.copy(isListening = false)
    }

    fun setLastExecutedAction(actionDesc: String) {
        _voiceState.value = _voiceState.value.copy(lastExecutedAction = actionDesc)
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
