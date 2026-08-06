package com.macsense.ai.util

/**
 * Generic bounded undo/redo history. Callers push the *previous* state before mutating current
 * state, then call [undo]/[redo] with the *current* state so it can be moved to the opposite
 * stack. This mirrors the classic editor undo pattern used by DAWs: every undoable action first
 * snapshots what existed before the edit.
 *
 * Bounded by [capacity] so autosave/undo history can't grow unbounded across a long session.
 */
class UndoRedoManager<T>(private val capacity: Int = 50) {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Record [previousState] as an undo point and clear any redo history (new branch of edits). */
    fun push(previousState: T) {
        undoStack.addLast(previousState)
        if (undoStack.size > capacity) undoStack.removeFirst()
        redoStack.clear()
    }

    /** Pops the most recent undo point, pushing [currentState] onto the redo stack. */
    fun undo(currentState: T): T? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(currentState)
        if (redoStack.size > capacity) redoStack.removeFirst()
        return previous
    }

    /** Pops the most recent redo point, pushing [currentState] back onto the undo stack. */
    fun redo(currentState: T): T? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(currentState)
        if (undoStack.size > capacity) undoStack.removeFirst()
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
