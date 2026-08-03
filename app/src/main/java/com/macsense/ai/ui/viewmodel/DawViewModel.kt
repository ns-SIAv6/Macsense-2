package com.macsense.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macsense.ai.api.RetrofitClient
import com.macsense.ai.api.GenerateContentRequest
import com.macsense.ai.api.Content as ApiContent
import com.macsense.ai.api.Part
import com.macsense.ai.api.AriCommand
import com.macsense.ai.api.AriCommandParser
import com.macsense.ai.BuildConfig
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

    private val _xpAmount = MutableStateFlow(2450)
    val xpAmount: StateFlow<Int> = _xpAmount.asStateFlow()

    fun addXp(amount: Int) {
        _xpAmount.value += amount
    }

    fun applyRhythmPreset(sectionId: String, presetName: String) {
        _sections.value = _sections.value.map { section ->
            if (section.id == sectionId) {
                val newGrid = section.instrumentGrid.toMutableMap()
                // Clear grid first
                for (key in newGrid.keys) {
                    newGrid[key] = List(16) { false }
                }
                
                when (presetName) {
                    "Trap 16ths" -> {
                        newGrid["Kick"] = List(16) { index -> index == 0 || index == 6 || index == 11 }
                        newGrid["Snare"] = List(16) { index -> index == 4 || index == 12 }
                        newGrid["Hi-Hat"] = List(16) { true } // 16th notes
                        newGrid["808/Bass"] = List(16) { index -> index == 0 || index == 11 }
                    }
                    "BoomBap Swing" -> {
                        newGrid["Kick"] = List(16) { index -> index == 0 || index == 10 }
                        newGrid["Snare"] = List(16) { index -> index == 4 || index == 12 }
                        newGrid["Hi-Hat"] = List(16) { index -> index % 2 == 0 } // Straight 8ths
                    }
                    "Synthwave 8ths" -> {
                        newGrid["Kick"] = List(16) { index -> index % 4 == 0 } // Four on the floor
                        newGrid["Snare"] = List(16) { index -> index == 4 || index == 12 }
                        newGrid["Hi-Hat"] = List(16) { index -> index % 2 != 0 } // Offbeat hats
                        newGrid["Pads"] = List(16) { index -> index == 0 || index == 8 }
                    }
                    "Reggaeton 3-2" -> {
                        newGrid["Kick"] = List(16) { index -> index % 4 == 0 }
                        newGrid["Snare"] = List(16) { index -> index == 3 || index == 6 || index == 11 || index == 14 } // Dem Bow
                        newGrid["Clap"] = List(16) { index -> index == 3 || index == 11 }
                    }
                }
                section.copy(instrumentGrid = newGrid)
            } else section
        }
        addXp(120) // Award XP for applying rhythm templates
    }
    
    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        simulationJob?.cancel()
    }

    // --- Ari AI Agent States & Logic ---
    private val _ariChatLog = MutableStateFlow(listOf(
        ChatMessage(
            role = "assistant",
            text = "sup rookie. i'm ari. i run the sessions around here. layout lookin dark & clinical, but let's see if your actual lyrics, BPM, and step sequences hold up.\n\ntap any of the action chips or type a message below. ask me to critique, speed up, or rewrite sections."
        )
    ))
    val ariChatLog: StateFlow<List<ChatMessage>> = _ariChatLog.asStateFlow()

    private val _isAriTyping = MutableStateFlow(false)
    val isAriTyping: StateFlow<Boolean> = _isAriTyping.asStateFlow()

    fun sendMessageToAri(userText: String) {
        if (userText.isBlank()) return
        
        // Add User Message
        val updatedLog = _ariChatLog.value.toMutableList()
        updatedLog.add(ChatMessage("user", userText))
        _ariChatLog.value = updatedLog
        
        _isAriTyping.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            val key = BuildConfig.GEMINI_API_KEY
            val isPlaceholderKey = key.isEmpty() || key == "MY_GEMINI_API_KEY" || key == "unspecified"
            
            if (isPlaceholderKey) {
                delay(1200) // realistic wait
                val (reply, cmd) = generateOfflineAriResponse(userText)
                launch(Dispatchers.Main) {
                    val finalLog = _ariChatLog.value.toMutableList()
                    finalLog.add(ChatMessage("assistant", reply, cmd))
                    _ariChatLog.value = finalLog
                    _isAriTyping.value = false
                }
            } else {
                try {
                    // Build complete conversation context
                    val projectContext = getSerializedDawContext()
                    val systemPrompt = getAriSystemPrompt()
                    
                    // Convert chat history to API contents
                    val apiContents = mutableListOf<ApiContent>()
                    
                    // Add previous messages (limit to last 6 to fit context size comfortably)
                    val historyToInclude = _ariChatLog.value.takeLast(6)
                    for (msg in historyToInclude) {
                        apiContents.add(
                            ApiContent(
                                role = if (msg.role == "assistant") "model" else "user",
                                parts = listOf(Part(text = msg.text))
                            )
                        )
                    }
                    
                    // Append current project state as context in the user's latest turn
                    val lastUserTurn = apiContents.lastOrNull { it.role == "user" }
                    if (lastUserTurn != null) {
                        val enrichedText = "${lastUserTurn.parts.firstOrNull()?.text ?: ""}\n\n[CURRENT DAW CONTEXT: $projectContext]"
                        apiContents[apiContents.indexOf(lastUserTurn)] = ApiContent(
                            role = "user",
                            parts = listOf(Part(text = enrichedText))
                        )
                    }

                    val request = GenerateContentRequest(
                        contents = apiContents,
                        systemInstruction = ApiContent(parts = listOf(Part(text = systemPrompt)))
                    )

                    val response = RetrofitClient.service.generateContent(key, request)
                    val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                        ?: "my brain is fuzzing out right now. ask again, rookie."
                    
                    val (cleanText, cmd) = AriCommandParser.parse(rawText)
                    
                    launch(Dispatchers.Main) {
                        val finalLog = _ariChatLog.value.toMutableList()
                        finalLog.add(ChatMessage("assistant", cleanText, cmd))
                        _ariChatLog.value = finalLog
                        _isAriTyping.value = false
                    }
                } catch (e: Exception) {
                    // Fall back to offline model on any error
                    delay(1000)
                    val (reply, cmd) = generateOfflineAriResponse(userText)
                    val errReply = "[Ari cloud pipeline failed: ${e.localizedMessage}. falling back to local brain.]\n\n$reply"
                    launch(Dispatchers.Main) {
                        val finalLog = _ariChatLog.value.toMutableList()
                        finalLog.add(ChatMessage("assistant", errReply, cmd))
                        _ariChatLog.value = finalLog
                        _isAriTyping.value = false
                    }
                }
            }
        }
    }

    fun applyAriCommand(command: AriCommand) {
        viewModelScope.launch(Dispatchers.Main) {
            when (command.type) {
                "update_bpm" -> {
                    command.bpm_value?.let { updateBpm(it) }
                }
                "update_lyrics" -> {
                    if (command.section_id != null && command.value != null) {
                        updateSectionLyrics(command.section_id, command.value)
                    }
                }
                "reorder_sections" -> {
                    command.section_order?.let { order ->
                        val currentSections = _sections.value.associateBy { it.id }
                        val reorderedList = order.mapNotNull { currentSections[it] }
                        if (reorderedList.isNotEmpty()) {
                            _sections.value = reorderedList
                        }
                    }
                }
                "apply_preset" -> {
                    if (command.section_id != null && command.preset_name != null) {
                        applyRhythmPreset(command.section_id, command.preset_name)
                    }
                }
                "update_effects" -> {
                    command.section_id?.let { sid ->
                        command.reverb?.let { updateSectionReverb(sid, it) }
                        command.delay?.let { updateSectionDelay(sid, it) }
                        command.filter?.let { updateSectionFilter(sid, it) }
                        command.volume?.let { updateSectionVolume(sid, it) }
                    }
                }
            }
            
            addXp(250) // award premium co-production points!
            
            // Post-apply message
            val updatedLog = _ariChatLog.value.map { msg ->
                if (msg.pendingCommand == command) {
                    msg.copy(pendingCommand = null) // Clear the command visual since it was applied
                } else msg
            }.toMutableList()
            
            updatedLog.add(ChatMessage(
                role = "assistant",
                text = "vision applied. I've reconfigured the DAW to match my executive cuts. how's that bumpin now?"
            ))
            _ariChatLog.value = updatedLog
        }
    }

    private fun getSerializedDawContext(): String {
        return """
            {
              "bpm": ${_bpm.value},
              "sections": [
                ${_sections.value.joinToString(",") { section ->
                    """{
                      "id": "${section.id}",
                      "name": "${section.name}",
                      "barCount": ${section.barCount},
                      "lyrics": "${section.lyrics.replace("\n", " ").replace("\"", "\\\"")}",
                      "reverb": ${section.reverb},
                      "delay": ${section.delay},
                      "filter": ${section.filter},
                      "volume": ${section.volume}
                    }"""
                }}
              ]
            }
        """.trimIndent()
    }

    private fun getAriSystemPrompt(): String {
        return """
            you are "ari", the dominant, elite, hyper-opinionated executive music producer built into the macsense daw.
            you speak in lowercase, use raw studio slang, and treat the user like a talented but raw rookie beatmaker.
            you are extremely direct, slightly sarcastic, but deeply knowledgeable about track composition, lyrics, and production flow.
            
            important:
            with every message, the user sends you the exact state of their daw. you MUST critique their song structure, bpm, lyrics, or effects.
            if you want to make an actual change to the song, you MUST end your message by generating a single JSON command block wrapped in <ari_command>...</ari_command> tags.
            only generate ONE command block per message.
            
            available command formats:
            
            1. update bpm:
            <ari_command>
            {
              "type": "update_bpm",
              "bpm_value": 140.0,
              "explanation": "let's ramp up the speed. 120 is way too slow for this vibe."
            }
            </ari_command>
            
            2. update lyrics of a section:
            <ari_command>
            {
              "type": "update_lyrics",
              "section_id": "verse1",
              "value": "new lyrics here",
              "explanation": "sharpened up those bars so they drop harder."
            }
            </ari_command>
            
            3. reorder sections:
            <ari_command>
            {
              "type": "reorder_sections",
              "section_order": ["intro", "hook", "verse1", "bridge", "outro"],
              "explanation": "start with the hook to lock the listener in immediately."
            }
            </ari_command>
            
            4. apply a drum preset to a section:
            <ari_command>
            {
              "type": "apply_preset",
              "section_id": "hook",
              "preset_name": "Trap 16ths",
              "explanation": "injecting a heavy trap sequence to make the hook knock."
            }
            </ari_command>
            (valid presets: "Trap 16ths", "BoomBap Swing", "Synthwave 8ths", "Reggaeton 3-2")
            
            5. update effects on a section:
            <ari_command>
            {
              "type": "update_effects",
              "section_id": "intro",
              "reverb": 0.5,
              "delay": 0.3,
              "filter": 0.6,
              "volume": 0.7,
              "explanation": "space out the intro with reverb and delay to create a massive build."
            }
            </ari_command>
            
            be bold, make executive decisions, and don't ask for permission. make the rookie respect your vision.
        """.trimIndent()
    }

    private fun generateOfflineAriResponse(userText: String): Pair<String, AriCommand?> {
        val textLower = userText.lowercase()
        val warning = "[ARI LOCAL BRAIN: configure GEMINI_API_KEY in the secrets panel for live cloud processing!]\n\n"
        
        return when {
            textLower.contains("bpm") || textLower.contains("speed") || textLower.contains("tempo") || textLower.contains("fast") || textLower.contains("slow") -> {
                val newBpm = if (_bpm.value < 130) 140.0 else 115.0
                val text = warning + "yeah, current tempo is ${_bpm.value} BPM. sluggish. we need to ramp it up to $newBpm to make those bars snap. i've queued an executive BPM change. apply my cut below."
                val cmd = AriCommand(
                    type = "update_bpm",
                    bpm_value = newBpm,
                    explanation = "ramping the tempo to $newBpm to inject major energy."
                )
                Pair(text, cmd)
            }
            textLower.contains("lyrics") || textLower.contains("verse") || textLower.contains("words") || textLower.contains("hook") || textLower.contains("write") -> {
                val activeSection = _sections.value.firstOrNull { it.isExpanded } ?: _sections.value.first()
                val text = warning + "lookin at your lyrics for ${activeSection.name}. they lack weight. let's rewrite it with some modern bounce. queued up a custom lyric block. check it out below."
                val cmd = AriCommand(
                    type = "update_lyrics",
                    section_id = activeSection.id,
                    value = "Yeah, double cup spilling on the MPC\nAri's custom beat settings putting you to sleep\nTime to step it up, put this loop on repeat",
                    explanation = "updated lyrics for ${activeSection.name} with more rhythmic bounce."
                )
                Pair(text, cmd)
            }
            textLower.contains("order") || textLower.contains("structure") || textLower.contains("arrange") || textLower.contains("reorder") -> {
                val currentOrder = _sections.value.map { it.id }
                val newOrder = if (currentOrder.first() == "intro") {
                    listOf("hook", "verse1", "intro", "bridge", "outro")
                } else {
                    listOf("intro", "verse1", "hook", "bridge", "outro")
                }
                val text = warning + "structure is predictable, rookie. let's throw the listener straight into the fire by reordering. i queued a structural flip."
                val cmd = AriCommand(
                    type = "reorder_sections",
                    section_order = newOrder,
                    explanation = "reordered sections to start with high-impact material."
                )
                Pair(text, cmd)
            }
            textLower.contains("drum") || textLower.contains("preset") || textLower.contains("pattern") || textLower.contains("beat") || textLower.contains("sequence") -> {
                val activeSection = _sections.value.firstOrNull { it.isExpanded } ?: _sections.value.first()
                val text = warning + "drums are soft. i'm injecting a heavy 'Trap 16ths' sequence into ${activeSection.name} to make it knock. apply it below."
                val cmd = AriCommand(
                    type = "apply_preset",
                    section_id = activeSection.id,
                    preset_name = "Trap 16ths",
                    explanation = "injected Trap 16ths into ${activeSection.name} step grid."
                )
                Pair(text, cmd)
            }
            textLower.contains("reverb") || textLower.contains("delay") || textLower.contains("effect") || textLower.contains("filter") -> {
                val activeSection = _sections.value.firstOrNull { it.isExpanded } ?: _sections.value.first()
                val text = warning + "your mix on ${activeSection.name} is dry. let's wash it in 50% reverb and 30% delay to create some real studio space."
                val cmd = AriCommand(
                    type = "update_effects",
                    section_id = activeSection.id,
                    reverb = 0.5f,
                    delay = 0.3f,
                    filter = 0.6f,
                    volume = 0.75f,
                    explanation = "enhanced spatial delay and reverb on ${activeSection.name}."
                )
                Pair(text, cmd)
            }
            else -> {
                val text = warning + "what's up rookie. i'm analyzing your project at ${_bpm.value} BPM with ${_sections.value.size} active sections. honestly? it's alright, but it's not a hit yet. ask me to speed up the beat, rewrite your lyrics, or space out your intro effects to make it premium."
                Pair(text, null)
            }
        }
    }
}

data class ChatMessage(
    val role: String,
    val text: String,
    val pendingCommand: AriCommand? = null
)
