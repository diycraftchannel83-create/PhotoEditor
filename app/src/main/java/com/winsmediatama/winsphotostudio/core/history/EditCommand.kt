package com.winsmediatama.winsphotostudio.core.history

/** Command contract for non-destructive, reversible editing operations. */
interface EditCommand {
    val label: String
    fun apply()
    fun undo()
}

/** Bounded command history avoids storing full-resolution bitmap snapshots per operation. */
class CommandHistory(private val maxEntries: Int = 100) {
    init { require(maxEntries > 0) { "History capacity must be positive" } }

    private val undoStack = ArrayDeque<EditCommand>()
    private val redoStack = ArrayDeque<EditCommand>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoLabel: String? get() = undoStack.lastOrNull()?.label
    val redoLabel: String? get() = redoStack.lastOrNull()?.label

    /** Failed apply leaves both stacks unchanged. */
    fun execute(command: EditCommand) {
        command.apply()
        undoStack.addLast(command)
        while (undoStack.size > maxEntries) undoStack.removeFirst()
        redoStack.clear()
    }

    /** Keep the command on its original stack until its reverse action succeeds. */
    fun undo() {
        val command = undoStack.lastOrNull() ?: return
        command.undo()
        undoStack.removeLast()
        redoStack.addLast(command)
    }

    /** Keep redo entry until apply succeeds, avoiding history loss on exceptions. */
    fun redo() {
        val command = redoStack.lastOrNull() ?: return
        command.apply()
        redoStack.removeLast()
        undoStack.addLast(command)
        while (undoStack.size > maxEntries) undoStack.removeFirst()
    }

    fun clear() { undoStack.clear(); redoStack.clear() }
}
