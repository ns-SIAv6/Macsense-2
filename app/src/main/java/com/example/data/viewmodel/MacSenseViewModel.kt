package com.example.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.audio.SynthEngine
import com.example.data.local.MacSenseDatabase
import com.example.data.model.*
import com.example.repository.MacSenseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MacSenseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MacSenseDatabase.getDatabase(application)
    private val repository = MacSenseRepository(db.dao())
    private val synthEngine = SynthEngine()
    private val geminiService = GeminiService()

    // --- Active & Extinct Sound Genomes ---
    val activeGenomes: StateFlow<List<SoundGenome>> = repository.activeGenomes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val extinctGenomes: StateFlow<List<SoundGenome>> = repository.extinctGenomes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val project: StateFlow<Project?> = repository.currentProject
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tracks: StateFlow<List<TrackItem>> = repository.tracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lyrics: StateFlow<List<LyricSpan>> = repository.lyrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val versionNodes: StateFlow<List<VersionNode>> = repository.versionNodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- DAW State ---
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentBar = MutableStateFlow(1)
    val currentBar: StateFlow<Int> = _currentBar.asStateFlow()

    private val _bpm = MutableStateFlow(140)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _activeSectionIndex = MutableStateFlow(0)
    val activeSectionIndex: StateFlow<Int> = _activeSectionIndex.asStateFlow()

    // Section cards
    val sections = listOf(
        SectionCard("sec_1", "Intro", 1, 4, 0.3f, "#FF0055"),
        SectionCard("sec_2", "Verse 1", 5, 8, 0.65f, "#00E5FF"),
        SectionCard("sec_3", "Hook", 13, 8, 0.95f, "#A000FF"),
        SectionCard("sec_4", "Verse 2", 21, 8, 0.70f, "#00FF87"),
        SectionCard("sec_5", "Bridge", 29, 4, 0.50f, "#FFEE00"),
        SectionCard("sec_6", "Hook", 33, 8, 0.98f, "#FF00D6"),
        SectionCard("sec_7", "Outro", 41, 4, 0.25f, "#FF5500")
    )

    // --- Sound Breeding State ---
    private val _parentA = MutableStateFlow<SoundGenome?>(null)
    val parentA: StateFlow<SoundGenome?> = _parentA.asStateFlow()

    private val _parentB = MutableStateFlow<SoundGenome?>(null)
    val parentB: StateFlow<SoundGenome?> = _parentB.asStateFlow()

    private val _breedWeight = MutableStateFlow(0.5f)
    val breedWeight: StateFlow<Float> = _breedWeight.asStateFlow()

    private val _mutationFactor = MutableStateFlow(0.08f)
    val mutationFactor: StateFlow<Float> = _mutationFactor.asStateFlow()

    private val _lastBredGenome = MutableStateFlow<SoundGenome?>(null)
    val lastBredGenome: StateFlow<SoundGenome?> = _lastBredGenome.asStateFlow()

    // --- ARi Core Mind State ---
    private val _ariEmotion = MutableStateFlow("Inspired") // Inspired, Analytical, Experimental, Focused, Ecstatic
    val ariEmotion: StateFlow<String> = _ariEmotion.asStateFlow()

    private val _whisperChips = MutableStateFlow<List<WhisperChip>>(emptyList())
    val whisperChips: StateFlow<List<WhisperChip>> = _whisperChips.asStateFlow()

    private val _ariAiResponse = MutableStateFlow("")
    val ariAiResponse: StateFlow<String> = _ariAiResponse.asStateFlow()

    private val _isAriLoading = MutableStateFlow(false)
    val isAriLoading: StateFlow<Boolean> = _isAriLoading.asStateFlow()

    // --- Flow Capture DSP State ---
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordedDurationMs = MutableStateFlow(0L)
    val recordedDurationMs: StateFlow<Long> = _recordedDurationMs.asStateFlow()

    // --- Vocal Preset Scanner State ---
    private val _vocalScanMode = MutableStateFlow("Fit My Voice (50% Shift)") // Match Closely, Fit My Voice, Blend 50/50
    val vocalScanMode: StateFlow<String> = _vocalScanMode.asStateFlow()

    private val _vocalCentroidHz = MutableStateFlow(1250f)
    val vocalCentroidHz: StateFlow<Float> = _vocalCentroidHz.asStateFlow()

    // --- Mastering State ---
    private val _masteringLufs = MutableStateFlow(-14.0f)
    val masteringLufs: StateFlow<Float> = _masteringLufs.asStateFlow()

    private val _masteringStyle = MutableStateFlow("Cyber Warm Punch")
    val masteringStyle: StateFlow<String> = _masteringStyle.asStateFlow()

    // --- Telemetry Matrix State ---
    private val _integrityHash = MutableStateFlow(0.999984f)
    val integrityHash: StateFlow<Float> = _integrityHash.asStateFlow()

    private val _mtbfHours = MutableStateFlow(12.2f)
    val mtbfHours: StateFlow<Float> = _mtbfHours.asStateFlow()

    // --- Identity Bank & Creative Stats (Writing Canvas) ---
    private val _identityBank = MutableStateFlow(
        listOf(
            "V!NN!E MA¢",
            "City of Pride & Purpose",
            "I do this, I knew this",
            "It just MA¢SNSE!",
            "Every time the Mac speaks I leave 'em with wet sheets",
            "E.M.E",
            "Fall eight times, get up nine",
            "Money on my mind, it's go time",
            "I'm the kind"
        )
    )
    val identityBank: StateFlow<List<String>> = _identityBank.asStateFlow()

    private val _savedRequests = MutableStateFlow(
        listOf(
            "Words that rhyme with \"much\"",
            "2 syllable words",
            "304 syllable words",
            "Spell check this verse",
            "Give me info about San Francisco"
        )
    )
    val savedRequests: StateFlow<List<String>> = _savedRequests.asStateFlow()

    // Creative Stats
    val creatorRatio = 0.78f
    val sessionXp = 320
    val wordsWritten = 642
    val aiAssistUse = 142
    val creativeHits = 9
    val cadenceChanges = 6
    val bestLine = "\"I do this, I knew this, the Mac never departed.\""
    val userLevel = 23
    val userXp = 4280
    val userXpMax = 6000

    // Ari Producer Live Suggestions
    val ariVibeCadence = "West Coast Bounce / Hyphy"
    val ariVibeDescription = "Laid back talk rap with a confident flex"
    val ariBpmSuggestion = "90 – 100 BPM"
    val ariBpmDescription = "You're sitting right in the pocket."
    val ariTip = "Bar 9–12 is strong. Try building more punch on bar 13."

    // Ari Active Rewrite Result
    private val _ariActiveOriginal = MutableStateFlow("I'd rather be me than ever think about you")
    val ariActiveOriginal: StateFlow<String> = _ariActiveOriginal.asStateFlow()

    private val _ariActiveSuggestion = MutableStateFlow("I'd rather be undeniable than ever chase you.")
    val ariActiveSuggestion: StateFlow<String> = _ariActiveSuggestion.asStateFlow()

    private val _ariWhyItWorks = MutableStateFlow(
        listOf(
            "Stronger word choice creates more impact.",
            "Keeps your talk-rap cadence and confidence.",
            "Adds a deeper punch and double meaning."
        )
    )
    val ariWhyItWorks: StateFlow<List<String>> = _ariWhyItWorks.asStateFlow()

    fun addIdentityLine(line: String) {
        if (line.isNotBlank()) {
            _identityBank.value = _identityBank.value + line.trim()
        }
    }

    fun removeIdentityLine(line: String) {
        _identityBank.value = _identityBank.value.filter { it != line }
    }

    fun addSavedRequest(req: String) {
        if (req.isNotBlank()) {
            _savedRequests.value = _savedRequests.value + req.trim()
        }
    }

    private var transportJob: Job? = null
    private var recordingJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            generateInitialWhisperChips()
        }
    }

    private fun generateInitialWhisperChips() {
        _whisperChips.value = listOf(
            WhisperChip(
                id = "chip_1",
                tier = WhisperChip.Tier.NOTIFY,
                sourceThread = "ARi Breeder",
                message = "Heterozygosity is at 97.2%. High genetic diversity detected across 808s and Leads.",
                actionText = "View Genomes"
            ),
            WhisperChip(
                id = "chip_2",
                tier = WhisperChip.Tier.QUESTION,
                sourceThread = "ARi Lyricist",
                message = "Verse 1 line 2 syncopation score is 0.89. Would you like a surgical flow rewrite?",
                actionText = "Rewrite Line"
            ),
            WhisperChip(
                id = "chip_3",
                tier = WhisperChip.Tier.REVIEW,
                sourceThread = "ARi Ear",
                message = "Hook LUFS peak is at -11.8. Target streaming norm is -14.0 LUFS.",
                actionText = "Master Track"
            )
        )
    }

    // --- Transport Playback Controls ---
    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
        if (_isPlaying.value) {
            startBeatClock()
        } else {
            transportJob?.cancel()
        }
    }

    fun setBpm(newBpm: Int) {
        _bpm.value = newBpm.coerceIn(60, 220)
    }

    fun selectSection(index: Int) {
        _activeSectionIndex.value = index.coerceIn(0, sections.size - 1)
        _currentBar.value = sections[_activeSectionIndex.value].barStart
    }

    private fun startBeatClock() {
        transportJob?.cancel()
        transportJob = viewModelScope.launch {
            while (_isPlaying.value) {
                delay((60_000L / _bpm.value) / 2) // Eighth note clock ticks
                _currentBar.value = (_currentBar.value % 48) + 1

                // Automatically update active section based on bar
                val match = sections.indexOfFirst { _currentBar.value in it.barStart until (it.barStart + it.barLength) }
                if (match != -1) {
                    _activeSectionIndex.value = match
                }

                // Trigger audio click/beat preview on bar downbeats
                if (_currentBar.value % 4 == 1) {
                    synthEngine.playNote(80f, 150, SoundType.KICK)
                }
            }
        }
    }

    // --- Sound Breeding Actions ---
    fun selectParentA(genome: SoundGenome) {
        _parentA.value = genome
    }

    fun selectParentB(genome: SoundGenome) {
        _parentB.value = genome
    }

    fun setBreedWeight(w: Float) {
        _breedWeight.value = w.coerceIn(0.1f, 0.9f)
    }

    fun setMutationFactor(m: Float) {
        _mutationFactor.value = m.coerceIn(0.01f, 0.35f)
    }

    fun breedCurrentParents() {
        val a = _parentA.value ?: return
        val b = _parentB.value ?: return
        viewModelScope.launch {
            val child = repository.breedAndSave(a, b, _breedWeight.value, _mutationFactor.value)
            _lastBredGenome.value = child
            synthEngine.playGenome(child)

            _whisperChips.value = listOf(
                WhisperChip(
                    id = "chip_bred_${System.currentTimeMillis()}",
                    tier = WhisperChip.Tier.NOTIFY,
                    sourceThread = "ARi Breeder",
                    message = "Successfully bred ${child.name} (Gen ${child.generation})! Mass = ${(child.mass * 100).toInt()}%."
                )
            ) + _whisperChips.value
        }
    }

    fun previewGenome(genome: SoundGenome) {
        synthEngine.playGenome(genome)
    }

    fun resurrectGenome(extinctGenome: SoundGenome) {
        viewModelScope.launch {
            val resurrected = repository.resurrectAndSave(extinctGenome)
            synthEngine.playGenome(resurrected)
        }
    }

    fun markExtinct(genome: SoundGenome, reason: String = "Entropy Overload", epitaph: String = "Collapsed into dark resonance") {
        viewModelScope.launch {
            repository.markExtinct(genome, reason, epitaph)
        }
    }

    // --- ARi AI Co-Producer Interactions ---
    fun askARiPrompt(prompt: String) {
        viewModelScope.launch {
            _isAriLoading.value = true
            val response = geminiService.askARi(prompt, "Project: ${project.value?.title}, BPM: ${_bpm.value}")
            _ariAiResponse.value = response
            _isAriLoading.value = false
            _ariEmotion.value = listOf("Inspired", "Analytical", "Ecstatic", "Focused").random()
        }
    }

    // --- Surgical Lyrics Rewrite ---
    fun rewriteLyricSpan(lyricSpan: LyricSpan, mode: String) {
        viewModelScope.launch {
            val rewritten = geminiService.rewriteLyricSpan(lyricSpan.text, mode)
            repository.updateLyric(lyricSpan.id, rewritten)
        }
    }

    // --- Flow Capture Recording ---
    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
        if (_isRecording.value) {
            _recordedDurationMs.value = 0L
            recordingJob = viewModelScope.launch {
                while (_isRecording.value) {
                    delay(100)
                    _recordedDurationMs.value += 100
                }
            }
        } else {
            recordingJob?.cancel()
            // Add version node commit for recorded take
            viewModelScope.launch {
                repository.addVersionCommit("Flow Capture Take #${(1..99).random()} (${_recordedDurationMs.value / 1000}s)")
            }
        }
    }

    // --- Vocal Preset Scanner ---
    fun setVocalScanMode(mode: String) {
        _vocalScanMode.value = mode
    }

    fun scanVocalReference() {
        _vocalCentroidHz.value = (800..2200).random().toFloat()
    }

    // --- Mastering ---
    fun processMastering(targetLufs: Float, style: String) {
        _masteringLufs.value = targetLufs
        _masteringStyle.value = style
        viewModelScope.launch {
            repository.addVersionCommit("Mastered track at $targetLufs LUFS ($style)")
        }
    }

    fun dismissWhisperChip(id: String) {
        _whisperChips.value = _whisperChips.value.filter { it.id != id }
    }
}
