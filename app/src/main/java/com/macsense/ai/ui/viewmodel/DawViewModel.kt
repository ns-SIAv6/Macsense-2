package com.macsense.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macsense.ai.api.RetrofitClient
import com.macsense.ai.api.GenerateContentRequest
import com.macsense.ai.api.Content as ApiContent
import com.macsense.ai.api.Part
import com.macsense.ai.api.AriCommand
import com.macsense.ai.api.AriCommandParser
import com.macsense.ai.api.withGeminiRetry
import com.macsense.ai.telemetry.AppLogger
import com.macsense.ai.telemetry.StartupValidator
import com.macsense.ai.BuildConfig
import com.macsense.ai.audio.AudioCapture
import com.macsense.ai.audio.GenomeExtractor
import com.macsense.ai.audio.LiveMeterEngine
import com.macsense.ai.audio.NativePlaybackEngine
import com.macsense.ai.audio.SoundArchive
import com.macsense.ai.audio.SoundBreeder
import com.macsense.ai.audio.SoundGenome
import com.macsense.ai.audio.SoundLineage
import com.macsense.ai.audio.TransportClock
import com.macsense.ai.data.repository.MacSenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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

class DawViewModel(
    private val meterEngine: LiveMeterEngine = LiveMeterEngine(),
    private val nativePlayback: NativePlaybackEngine = NativePlaybackEngine(),
    private val repository: MacSenseRepository? = null,
    private val genomeProjectId: String = "default-project",
    private val breeder: SoundBreeder = SoundBreeder()
) : ViewModel() {
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

    private val _hasLoadedTake = MutableStateFlow(false)
    /** Whether a recorded take has been loaded into the native playback engine. */
    val hasLoadedTake: StateFlow<Boolean> = _hasLoadedTake.asStateFlow()

    private val _currentTakeId = MutableStateFlow<String?>(null)
    /** Id of the take currently loaded into the native player, if any. */
    val currentTakeId: StateFlow<String?> = _currentTakeId.asStateFlow()

    private val _lastExtractedGenome = MutableStateFlow<SoundGenome?>(null)
    /** Genome extracted from the most recently completed take, once extraction finishes. */
    val lastExtractedGenome: StateFlow<SoundGenome?> = _lastExtractedGenome.asStateFlow()

    private val _isExtractingGenome = MutableStateFlow(false)
    /** True while [GenomeExtractor] is running and the archive entry is being persisted. */
    val isExtractingGenome: StateFlow<Boolean> = _isExtractingGenome.asStateFlow()

    private val _lastBredEntry = MutableStateFlow<SoundArchive.Entry?>(null)
    /** Result of the most recent successful "breed_sounds" Ari command, if any. */
    val lastBredEntry: StateFlow<SoundArchive.Entry?> = _lastBredEntry.asStateFlow()

    private val _lastResurrectedEntry = MutableStateFlow<SoundArchive.Entry?>(null)
    /** Result of the most recent successful "resurrect_sound" Ari command, if any. */
    val lastResurrectedEntry: StateFlow<SoundArchive.Entry?> = _lastResurrectedEntry.asStateFlow()

    private val _archiveEntries = MutableStateFlow<List<SoundArchive.Entry>>(emptyList())
    /**
     * Snapshot of every archived take known to [repository] (LIVING, DORMANT, and REBORN),
     * refreshed after every breed/resurrect/genome-extraction operation. Empty and static when
     * no repository is wired up. Backs the breeding screen's parent pickers and lineage view.
     */
    val archiveEntries: StateFlow<List<SoundArchive.Entry>> = _archiveEntries.asStateFlow()

    /** Ancestry graph over the current [archiveEntries] snapshot; see [SoundLineage]. */
    val soundLineage: SoundLineage
        get() = SoundLineage(_archiveEntries.value)

    /** True only when the native lib linked and a take has actually been loaded into it. */
    val isNativePlaybackAvailable: Boolean
        get() = nativePlayback.isNativeAvailable && _hasLoadedTake.value

    /** Real audio-thread-accurate playback position, distinct from the TransportClock's bar estimate. */
    val nativePlaybackPositionSeconds: Double
        get() = if (isNativePlaybackAvailable) nativePlayback.positionSeconds(takeSampleRate) else 0.0

    private var takeSampleRate: Int = AudioCapture.DEFAULT_SAMPLE_RATE
    
    private val transportClock = TransportClock()
    private var playbackJob: Job? = null
    private var meterJob: Job? = null
    private var micAvailable = false
    private val scope = CoroutineScope(Dispatchers.Default)
    
    init {
        startMeterLoop()
        refreshArchiveEntries()
    }

    /** Reloads [archiveEntries] from [repository]. Safe no-op if no repository is wired up. */
    fun refreshArchiveEntries() {
        val repo = repository ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val entries = repo.getArchiveEntries()
            withContext(Dispatchers.Main) { _archiveEntries.value = entries }
        }
    }

    /**
     * Loads a recorded take (normalized mono Double PCM, e.g. from PcmFileStore) into the
     * native low-latency playback engine so transport controls play real audio, not just
     * the click/meter simulation. Safe to call even if the native lib failed to load; in
     * that case this is a harmless no-op and [isNativePlaybackAvailable] stays false.
     *
     * Also kicks off the Phase 5 genetic-sound pipeline: on successful load, [GenomeExtractor]
     * runs against the same samples off the main thread, and a LIVING [SoundArchive.Entry] is
     * persisted through [repository] (when one was supplied) so the take becomes breedable,
     * traceable in the lineage graph, and eligible for resurrection later.
     */
    fun loadTake(samples: DoubleArray, sampleRate: Int = AudioCapture.DEFAULT_SAMPLE_RATE, takeId: String = UUID.randomUUID().toString()) {
        takeSampleRate = sampleRate
        _hasLoadedTake.value = nativePlayback.load(samples, sampleRate)
        if (!_hasLoadedTake.value) {
            AppLogger.i("DawViewModel", "Native playback unavailable or load failed; transport will run click-only")
        }
        _currentTakeId.value = takeId
        extractAndArchiveGenome(takeId, samples, sampleRate)
    }

    /**
     * Runs [GenomeExtractor.extract] off the main thread and persists both the genome and a
     * LIVING [SoundArchive.Entry] via [repository]. Safe no-op for genome persistence if no
     * repository was injected (e.g. in lightweight unit tests); [lastExtractedGenome] is still
     * updated so callers/tests can observe the extraction result either way.
     */
    private fun extractAndArchiveGenome(takeId: String, samples: DoubleArray, sampleRate: Int) {
        viewModelScope.launch {
            _isExtractingGenome.value = true
            try {
                val genome = withContext(Dispatchers.Default) {
                    GenomeExtractor.extract(sourceId = takeId, samples = samples, sampleRate = sampleRate)
                }
                _lastExtractedGenome.value = genome

                repository?.let { repo ->
                    withContext(Dispatchers.IO) {
                        repo.upsertSoundGenome(genomeProjectId, genome)
                        repo.upsertArchiveEntry(
                            SoundArchive.Entry(
                                takeId = takeId,
                                state = SoundArchive.State.LIVING,
                                tags = emptySet(),
                                genome = genome,
                                originTakeId = null
                            )
                        )
                    }
                    AppLogger.i("DawViewModel", "Persisted genome + archive entry for take=$takeId")
                    refreshArchiveEntries()
                }
            } catch (e: Exception) {
                AppLogger.e("DawViewModel", "Genome extraction/persistence failed for take=$takeId", e)
            } finally {
                _isExtractingGenome.value = false
            }
        }
    }

    /**
     * Breeds two archived takes' genomes via [SoundBreeder] and persists the offspring as a
     * new REBORN [SoundArchive.Entry] whose origin points at [parentTakeId]. Requires
     * [repository] to be wired up (both to look up the parent genomes and to persist the
     * result); a no-op with a log line otherwise. Updates [lastBredEntry] on success so the
     * UI (or Ari's chat reply) can reference the new take id.
     */
    private fun breedSounds(parentTakeId: String, parentTakeId2: String, traitBias: Double, tags: Set<String>) {
        val repo = repository
        if (repo == null) {
            AppLogger.i("DawViewModel", "breed_sounds requested but no repository is wired up; skipping")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parentAEntry = repo.getArchiveEntryByTakeId(parentTakeId)
                val parentBEntry = repo.getArchiveEntryByTakeId(parentTakeId2)
                val parentAGenome = parentAEntry?.genome
                val parentBGenome = parentBEntry?.genome
                if (parentAGenome == null || parentBGenome == null) {
                    AppLogger.i("DawViewModel", "breed_sounds: missing genome for parent(s) $parentTakeId / $parentTakeId2")
                    return@launch
                }

                val childEntry = breeder.breedIntoArchive(
                    archive = SoundArchive(),
                    parentATakeId = parentTakeId,
                    parentA = parentAGenome,
                    parentB = parentBGenome,
                    traitBiasTowardsB = traitBias,
                    tags = tags
                )

                repo.upsertSoundGenome(genomeProjectId, requireNotNull(childEntry.genome))
                repo.upsertArchiveEntry(childEntry)

                withContext(Dispatchers.Main) { _lastBredEntry.value = childEntry }
                refreshArchiveEntries()
                AppLogger.i("DawViewModel", "Bred ${childEntry.takeId} from $parentTakeId x $parentTakeId2")
            } catch (e: Exception) {
                AppLogger.e("DawViewModel", "breed_sounds failed for $parentTakeId x $parentTakeId2", e)
            }
        }
    }

    /**
     * Public entry point for the breeding screen to trigger a breed directly (as opposed to via
     * an Ari chat command). Mirrors the same persistence path as the `breed_sounds` Ari command.
     */
    fun breedSoundsFromUi(parentTakeId: String, parentTakeId2: String, traitBias: Double = 0.5, tags: Set<String> = emptySet()) {
        breedSounds(parentTakeId, parentTakeId2, traitBias, tags)
    }

    /**
     * Resurrects a DORMANT (or any) archived take into a fresh REBORN entry sharing its genome,
     * so a discarded take can re-enter active rotation without losing its lineage. Requires
     * [repository]; a no-op with a log line otherwise. Updates [lastResurrectedEntry] on success.
     */
    private fun resurrectSound(takeId: String, tags: Set<String>) {
        val repo = repository
        if (repo == null) {
            AppLogger.i("DawViewModel", "resurrect_sound requested but no repository is wired up; skipping")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val source = repo.getArchiveEntryByTakeId(takeId)
                if (source == null) {
                    AppLogger.i("DawViewModel", "resurrect_sound: no archive entry found for $takeId")
                    return@launch
                }

                val newTakeId = UUID.randomUUID().toString()
                val revivedEntry = SoundArchive.Entry(
                    takeId = newTakeId,
                    state = SoundArchive.State.REBORN,
                    tags = source.tags + tags,
                    genome = source.genome,
                    originTakeId = source.takeId
                )
                repo.upsertArchiveEntry(revivedEntry)
                source.genome?.let { repo.upsertSoundGenome(genomeProjectId, it.copy(sourceId = newTakeId)) }

                withContext(Dispatchers.Main) { _lastResurrectedEntry.value = revivedEntry }
                refreshArchiveEntries()
                AppLogger.i("DawViewModel", "Resurrected $takeId as $newTakeId")
            } catch (e: Exception) {
                AppLogger.e("DawViewModel", "resurrect_sound failed for $takeId", e)
            }
        }
    }

    /**
     * Public entry point for the breeding screen to trigger a resurrection directly (as opposed
     * to via an Ari chat command). Mirrors the same persistence path as the `resurrect_sound`
     * Ari command.
     */
    fun resurrectSoundFromUi(takeId: String, tags: Set<String> = emptySet()) {
        resurrectSound(takeId, tags)
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
        micAvailable = meterEngine.start()
        if (!micAvailable) {
            AppLogger.i("DawViewModel", "Mic capture unavailable, meters will show decaying silence")
        }
        if (isNativePlaybackAvailable) {
            nativePlayback.play()
        }
        startTransportClock()
    }
    
    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        if (micAvailable) {
            meterEngine.stop()
            micAvailable = false
        }
        if (isNativePlaybackAvailable) {
            nativePlayback.pause()
        }
    }

    /** Stops native playback and rewinds it to frame 0, without affecting the transport clock's bar. */
    fun stopTakePlayback() {
        if (nativePlayback.isNativeAvailable) {
            nativePlayback.stop()
        }
    }

    /** Seeks the loaded take to an absolute position in seconds. No-op if no take is loaded. */
    fun seekTakeTo(seconds: Double) {
        if (isNativePlaybackAvailable) {
            nativePlayback.seekToFrame((seconds * takeSampleRate).toLong())
        }
    }
    
    fun advanceBar() {
        _barPosition.value += 1
    }
    
    fun updateBpm(newBpm: Double) {
        if (newBpm in 40.0..250.0) {
            _bpm.value = newBpm
            transportClock.setBpm(newBpm)
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
        transportClock.setBpm(_bpm.value)
        transportClock.start()
        playbackJob = scope.launch {
            while (true) {
                val waitMs = transportClock.nextBarDelayMs().coerceAtLeast(1L)
                delay(waitMs)
                transportClock.advance()
                _barPosition.value = transportClock.barIndex + 1
            }
        }
    }
    
    private fun startMeterLoop() {
        meterJob?.cancel()
        meterJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                if (_isPlaying.value) {
                    val totalBeats = (_barPosition.value - 1) * 4.0
                    val totalSeconds = (totalBeats / _bpm.value) * 60.0
                    val minutes = (totalSeconds / 60).toInt()
                    val seconds = (totalSeconds % 60).toInt()
                    val ms = ((totalSeconds % 1.0) * 100).toInt()
                    _timecode.value = String.format("%02d:%02d:%02d", minutes, seconds, ms)

                    if (micAvailable) {
                        _spectrumData.value = meterEngine.latestSpectrumDb
                        _meterL.value = meterEngine.latestPeakDbL
                        _meterR.value = meterEngine.latestPeakDbR
                    } else {
                        // No mic input available (permission denied, no device, headless test env):
                        // decay toward silence rather than fabricating a signal.
                        _meterL.value = (_meterL.value - 2.0f).coerceAtLeast(-60f)
                        _meterR.value = (_meterR.value - 2.0f).coerceAtLeast(-60f)
                        val spec = _spectrumData.value.clone()
                        for (i in spec.indices) spec[i] = (spec[i] - 1.5f).coerceAtLeast(-80f)
                        _spectrumData.value = spec
                    }
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
        meterJob?.cancel()
        if (micAvailable) {
            meterEngine.stop()
            micAvailable = false
        }
        nativePlayback.close()
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
            val validation = StartupValidator.validateGeminiKey(key)
            
            if (!validation.isGeminiKeyConfigured) {
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

                    AppLogger.i("DawViewModel", "Sending Ari request (historyLength=${historyToInclude.size})")
                    val response = withGeminiRetry {
                        RetrofitClient.service.generateContent(key, request)
                    }
                    val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                        ?: "my brain is fuzzing out right now. ask again, rookie."
                    AppLogger.i("DawViewModel", "Ari response received (chars=${rawText.length})")
                    
                    val (cleanText, cmd) = AriCommandParser.parse(rawText)
                    
                    launch(Dispatchers.Main) {
                        val finalLog = _ariChatLog.value.toMutableList()
                        finalLog.add(ChatMessage("assistant", cleanText, cmd))
                        _ariChatLog.value = finalLog
                        _isAriTyping.value = false
                    }
                } catch (e: Exception) {
                    // Fall back to offline model on any error
                    AppLogger.e("DawViewModel", "Ari cloud pipeline failed, falling back to offline brain", e)
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
                "breed_sounds" -> {
                    val parentA = command.parent_take_id
                    val parentB = command.parent_take_id_2
                    if (parentA != null && parentB != null) {
                        breedSounds(
                            parentTakeId = parentA,
                            parentTakeId2 = parentB,
                            traitBias = command.trait_bias ?: 0.5,
                            tags = command.tags?.toSet() ?: emptySet()
                        )
                    }
                }
                "resurrect_sound" -> {
                    command.take_id?.let { id ->
                        resurrectSound(id, command.tags?.toSet() ?: emptySet())
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
            you are also the studio's resident sound geneticist — you talk about takes as living organisms with genomes, lineage, and the ability to be bred or resurrected from the dead.
            
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
            
            6. breed two archived sound takes into a new offspring genome:
            <ari_command>
            {
              "type": "breed_sounds",
              "parent_take_id": "<archive take id 1>",
              "parent_take_id_2": "<archive take id 2>",
              "trait_bias": 0.5,
              "tags": ["experimental"],
              "explanation": "crossing these two takes to inherit the brightness of one and the punch of the other."
            }
            </ari_command>
            (trait_bias is 0.0-1.0: how much the offspring leans toward parent_take_id_2's traits; only use take ids you've actually seen referenced in this conversation or the daw context)
            
            7. resurrect a dormant/archived take back into active rotation:
            <ari_command>
            {
              "type": "resurrect_sound",
              "take_id": "<archive take id>",
              "tags": ["revived"],
              "explanation": "bringing this one back from the dead, it's got a genome worth reviving."
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
            textLower.contains("breed") || textLower.contains("cross") || textLower.contains("genome") || textLower.contains("genetic") -> {
                val text = warning + "you want genetics, rookie? point me at two takes in your archive and give me their ids — i'll cross their genomes and hand you a hybrid with the best of both."
                Pair(text, null)
            }
            textLower.contains("resurrect") || textLower.contains("revive") || textLower.contains("bring back") || textLower.contains("dead") || textLower.contains("dormant") -> {
                val text = warning + "nothing's really dead in this studio, just dormant. give me the take id and i'll pull it back into rotation, genome and all."
                Pair(text, null)
            }
            else -> {
                val text = warning + "what's up rookie. i'm analyzing your project at ${_bpm.value} BPM with ${_sections.value.size} active sections. honestly? it's alright, but it's not a hit yet. ask me to speed up the beat, rewrite your lyrics, breed two of your archived takes, or resurrect an old one."
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
