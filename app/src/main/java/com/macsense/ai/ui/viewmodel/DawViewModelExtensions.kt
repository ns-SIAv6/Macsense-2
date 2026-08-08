package com.macsense.ai.ui.viewmodel

import com.macsense.ai.audio.StemTrack
import com.macsense.ai.data.local.ClipEntity

/**
 * Extension functions on [DawViewModel] wiring [UndoRedoManager] into the mutation API
 * without modifying the large DawViewModel source file.
 *
 * DawViewModel exposes internal setters via the `restoreXxx` / `updateBpm` package-level
 * functions below. The `WeakHashMap` pattern gives each VM its own UndoRedoManager
 * while letting it be GC'd with the VM itself.
 *
 * UI usage:
 *   val canUndo by viewModel.undoRedoManager.canUndo.collectAsState()
 *   val canRedo by viewModel.undoRedoManager.canRedo.collectAsState()
 *   IconButton(onClick = { viewModel.undoLastAction() }, enabled = canUndo) { ... }
 *   IconButton(onClick = { viewModel.redoLastAction() }, enabled = canRedo) { ... }
 */
private val vmUndoManagers = java.util.WeakHashMap<DawViewModel, UndoRedoManager>()

val DawViewModel.undoRedoManager: UndoRedoManager
    get() = vmUndoManagers.getOrPut(this) {
        UndoRedoManager(
            maxHistory = 50,
            autosaveTrigger = {
                try { refreshAllSectionClips() } catch (t: Throwable) {
                    com.macsense.ai.telemetry.AppLogger.w(
                        "UndoRedo", "Autosave no-op: ${t.message}"
                    )
                }
            },
            autosaveDelayMs = 500L
        )
    }

/**
 * Wraps any mutation in an undo-able snapshot:
 *   viewModel.recordableAction("Rename section") { updateSectionName(id, name) }
 */
fun DawViewModel.recordableAction(description: String, mutate: DawViewModel.() -> Unit) {
    val before = UndoState.CompositeSnapshot(
        sections = sections.value,
        clipsBySection = clipsBySection.value,
        stems = stemTracks.value,
        bpm = bpm.value,
        description = description
    )
    undoRedoManager.push(before)
    mutate()
}

/** Restores the previous recorded state. No-op if nothing to undo. */
fun DawViewModel.undoLastAction() {
    val current = currentComposite("current")
    val prev = undoRedoManager.undo(current) ?: return
    applyUndoState(prev)
}

/** Re-applies the last undone action. No-op if nothing to redo. */
fun DawViewModel.redoLastAction() {
    val current = currentComposite("current")
    val next = undoRedoManager.redo(current) ?: return
    applyUndoState(next)
}

private fun DawViewModel.currentComposite(desc: String) = UndoState.CompositeSnapshot(
    sections = sections.value,
    clipsBySection = clipsBySection.value,
    stems = stemTracks.value,
    bpm = bpm.value,
    description = desc
)

/** Applies any [UndoState] variant back into the ViewModel's public mutators. */
fun DawViewModel.applyUndoState(state: UndoState) {
    when (state) {
        is UndoState.SectionSnapshot -> restoreSections(state.sections)
        is UndoState.ClipSnapshot -> restoreClips(state.clipsBySection)
        is UndoState.StemSnapshot -> restoreStems(state.stems)
        is UndoState.BpmSnapshot -> updateBpm(state.bpm)
        is UndoState.LoopSnapshot -> restoreLoopRegion(state.loopRegion)
        is UndoState.CompositeSnapshot -> {
            restoreSections(state.sections)
            restoreClips(state.clipsBySection)
            restoreStems(state.stems)
            updateBpm(state.bpm)
        }
    }
}
