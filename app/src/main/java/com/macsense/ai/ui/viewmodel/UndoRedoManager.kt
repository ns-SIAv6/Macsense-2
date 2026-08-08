package com.macsense.ai.ui.viewmodel

import com.macsense.ai.audio.StemTrack
import com.macsense.ai.data.local.ClipEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Snapshot of all mutable DAW state at a single point in time. Used for undo/redo.
 *
 * Stored in a bounded [ArrayDeque] by [UndoRedoManager]; each push captures
 * the *before* state so that undo restores it exactly.
 */
sealed class UndoState {
 data class SectionSnapshot(
 val sections: List<SectionInfo>,
 val description: String = "Section change"
 ) : UndoState()

 data class ClipSnapshot(
 val clipsBySection: Map<String, List<ClipEntity>>,
 val description: String = "Clip change"
 ) : UndoState()

 data class StemSnapshot(
 val stems: List<StemTrack>,
 val description: String = "Stem change"
 ) : UndoState()

 data class BpmSnapshot(
 val bpm: Double,
 val description: String = "BPM change"
 ) : UndoState()

 data class LoopSnapshot(
 val loopRegion: Pair<Int, Int>?,
 val description: String = "Loop region change"
 ) : UndoState()

 data class CompositeSnapshot(
 val sections: List<SectionInfo>,
 val clipsBySection: Map<String, List<ClipEntity>>,
 val stems: List<StemTrack>,
 val bpm: Double,
 val description: String = "Action"
 ) : UndoState()
}

/**
 * Manages undo/redo history for [DawViewModel]. History is bounded at [maxHistory] states
 * to keep memory usage predictable regardless of session length.
 *
 * **Thread safety**: all public methods are expected to be called from the main thread
 * (via ViewModelScope). The autosave debounce launches on [Dispatchers.Main].
 */
class UndoRedoManager(
 private val maxHistory: Int = 50,
 private val autosaveTrigger: (suspend () -> Unit)? = null,
 private val autosaveDelayMs: Long = 500L
) {
 private val undoStack = ArrayDeque<UndoState>(maxHistory)
 private val redoStack = ArrayDeque<UndoState>(maxHistory)

 private val _canUndo = MutableStateFlow(false)
 val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

 private val _canRedo = MutableStateFlow(false)
 val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

 private val _undoCount = MutableStateFlow(0)
 val undoCount: StateFlow<Int> = _undoCount.asStateFlow()

 private val _lastDescription = MutableStateFlow<String?>(null)
 val lastDescription: StateFlow<String?> = _lastDescription.asStateFlow()

 private var autosaveJob: Job? = null
 private val autosaveScope = CoroutineScope(Dispatchers.Main)

 /**
 * Push the *before* state onto the undo stack before applying a mutation.
 * Clears redo stack since a new branch has started.
 */
 fun push(state: UndoState) {
 if (undoStack.size >= maxHistory) {
 undoStack.removeFirst() // evict oldest
 }
 undoStack.addLast(state)
 redoStack.clear()
 updateFlows()
 scheduleAutosave()
 }

 /**
 * Pops the last pushed state and returns it so [DawViewModel] can restore it.
 * Caller must push the *current* state onto [redoStack] before applying.
 */
 fun undo(currentState: UndoState): UndoState? {
 if (undoStack.isEmpty()) return null
 val toRestore = undoStack.removeLast()
 if (redoStack.size >= maxHistory) redoStack.removeFirst()
 redoStack.addLast(currentState)
 updateFlows()
 scheduleAutosave()
 return toRestore
 }

 /**
 * Pops the topmost redo state and returns it so [DawViewModel] can re-apply it.
 * Caller must push the *current* state onto [undoStack] before applying.
 */
 fun redo(currentState: UndoState): UndoState? {
 if (redoStack.isEmpty()) return null
 val toRestore = redoStack.removeLast()
 if (undoStack.size >= maxHistory) undoStack.removeFirst()
 undoStack.addLast(currentState)
 updateFlows()
 scheduleAutosave()
 return toRestore
 }

 /** Description of the next undoable action, or null. */
 fun peekUndoDescription(): String? = undoStack.lastOrNull()?.let {
 when (it) {
 is UndoState.SectionSnapshot -> it.description
 is UndoState.ClipSnapshot -> it.description
 is UndoState.StemSnapshot -> it.description
 is UndoState.BpmSnapshot -> it.description
 is UndoState.LoopSnapshot -> it.description
 is UndoState.CompositeSnapshot -> it.description
 }
 }

 /** Description of the next redoable action, or null. */
 fun peekRedoDescription(): String? = redoStack.lastOrNull()?.let {
 when (it) {
 is UndoState.SectionSnapshot -> it.description
 is UndoState.ClipSnapshot -> it.description
 is UndoState.StemSnapshot -> it.description
 is UndoState.BpmSnapshot -> it.description
 is UndoState.LoopSnapshot -> it.description
 is UndoState.CompositeSnapshot -> it.description
 }
 }

 fun clearHistory() {
 undoStack.clear()
 redoStack.clear()
 updateFlows()
 }

 private fun updateFlows() {
 _canUndo.value = undoStack.isNotEmpty()
 _canRedo.value = redoStack.isNotEmpty()
 _undoCount.value = undoStack.size
 _lastDescription.value = peekUndoDescription()
 }

 /** Debounced autosave — the last mutation within the window wins. */
 private fun scheduleAutosave() {
 val trigger = autosaveTrigger ?: return
 autosaveJob?.cancel()
 autosaveJob = autosaveScope.launch {
 delay(autosaveDelayMs)
 trigger()
 }
 }
}
