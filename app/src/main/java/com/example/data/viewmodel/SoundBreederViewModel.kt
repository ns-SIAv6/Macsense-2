package com.example.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MidiControllerManager
import com.example.audio.SynthEngine
import com.example.data.local.BreedingHistoryEntity
import com.example.data.local.MacSenseDatabase
import com.example.data.model.SoundGenome
import com.example.repository.MacSenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * SoundBreeder ViewModel for MA¢SENSE Studio.
 * Handles algorithmic combination of sound parameters, genetic breeding of audio traits,
 * real-time MIDI controller parameter manipulation, and Room Database persistence.
 */
class SoundBreederViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MacSenseDatabase.getDatabase(application)
    private val repository = MacSenseRepository(db.dao())
    private val synthEngine = SynthEngine()
    val midiManager = MidiControllerManager(application)

    // --- Active Genomes & Breeding History from Room ---
    val activeGenomes: StateFlow<List<SoundGenome>> = repository.activeGenomes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val breedingHistory: StateFlow<List<BreedingHistoryEntity>> = repository.breedingHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- MIDI Controller Live State ---
    val midiState: StateFlow<MidiControllerManager.MidiState> = midiManager.midiState

    // --- Breeding Parents & Trait Controls ---
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

    // --- Live MIDI Modulated Genome Trait Preview ---
    val liveModulatedGenome: StateFlow<SoundGenome?> = combine(
        _parentA, _parentB, _breedWeight, _mutationFactor, midiState
    ) { pA, pB, weight, mut, midi ->
        if (pA != null && pB != null) {
            val baseChild = SoundGenome.breed(pA, pB, weight, mut)
            // Apply live MIDI CC overrides if present
            baseChild.copy(
                mass = midi.paramMassOverride ?: baseChild.mass,
                radiance = midi.paramRadianceOverride ?: baseChild.radiance,
                entropy = midi.paramEntropyOverride ?: baseChild.entropy,
                curvature = midi.paramCurvatureOverride ?: baseChild.curvature,
                chrom1 = midi.paramChrom1FreqOverride ?: baseChild.chrom1,
                chrom5 = midi.paramChrom5DistortionOverride ?: baseChild.chrom5
            )
        } else pA ?: pB
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()

            // Observe MIDI notes to play live synth audio
            midiState.collect { midi ->
                val note = midi.lastNotePressed
                if (note != null && midi.lastVelocity > 0) {
                    val freq = midiManager.getNoteFrequency(note)
                    val genomeToPlay = liveModulatedGenome.value
                    if (genomeToPlay != null) {
                        synthEngine.playGenome(
                            genomeToPlay.copy(chrom1 = freq.toInt().coerceIn(30, 8000))
                        )
                    } else {
                        synthEngine.playNote(freq, 350)
                    }
                }
            }
        }
    }

    // --- Parent Selection & Breeding Controls ---
    fun selectParentA(genome: SoundGenome) {
        _parentA.value = genome
    }

    fun selectParentB(genome: SoundGenome) {
        _parentB.value = genome
    }

    fun setBreedWeight(weight: Float) {
        val clamped = weight.coerceIn(0.1f, 0.9f)
        _breedWeight.value = clamped
        midiManager.setVirtualCc(7, clamped)
    }

    fun setMutationFactor(factor: Float) {
        val clamped = factor.coerceIn(0.01f, 0.35f)
        _mutationFactor.value = clamped
        midiManager.setVirtualCc(17, clamped)
    }

    /**
     * Executes genetic breeding algorithm, saves new offspring to Room Database,
     * updates breeding history, and triggers audio preview.
     */
    fun breedCurrentParents() {
        val pA = _parentA.value ?: return
        val pB = _parentB.value ?: return
        viewModelScope.launch {
            val weight = midiState.value.paramBreedWeightOverride ?: _breedWeight.value
            val mut = midiState.value.paramMutationFactorOverride ?: _mutationFactor.value

            val child = repository.breedAndSave(pA, pB, weight, mut)

            // Apply any live MIDI overrides
            val finalChild = child.copy(
                mass = midiState.value.paramMassOverride ?: child.mass,
                radiance = midiState.value.paramRadianceOverride ?: child.radiance,
                entropy = midiState.value.paramEntropyOverride ?: child.entropy,
                curvature = midiState.value.paramCurvatureOverride ?: child.curvature
            )

            _lastBredGenome.value = finalChild
            synthEngine.playGenome(finalChild)
        }
    }

    fun previewGenome(genome: SoundGenome) {
        synthEngine.playGenome(genome)
    }

    // --- MIDI Controller Handling ---
    fun processMidiCc(ccNumber: Int, ccValue: Int) {
        midiManager.processMidiCc(ccNumber, ccValue)
    }

    fun processKeyEvent(keyCode: Int, isDown: Boolean): Boolean {
        return midiManager.processKeyEvent(keyCode, isDown)
    }

    fun setVirtualCc(ccNumber: Int, normalizedValue: Float) {
        midiManager.setVirtualCc(ccNumber, normalizedValue)
    }
}
