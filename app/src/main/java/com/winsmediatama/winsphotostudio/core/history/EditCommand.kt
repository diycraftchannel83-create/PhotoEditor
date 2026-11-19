package com.winsmediatama.winsphotostudio.core.history

/** Command contract for non-destructive, reversible editing operations. */
interface EditCommand {
    val label: String
    fun apply()
    fun undo()
}

/** Bounded command history avoids storing full-resolution bitmap snapshots per operation. */
class CommandHistory(private val maxEntries: Int = 100) {
    private val undoStack = ArrayDeque<EditCommand>()
    private val redoStack = ArrayDeque<EditCommand>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoLabel: String? get() = undoStack.lastOrNull()?.label
    val redoLabel: String? get() = redoStack.lastOrNull()?.label

    fun execute(command: EditCommand) {
        command.apply()
        undoStack.addLast(command)
        while (undoStack.size > maxEntries) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo() {
        val command = undoStack.removeLastOrNull() ?: return
        command.undo()
        redoStack.addLast(command)
    }

    fun redo() {
        val command = redoStack.removeLastOrNull() ?: return
        command.apply()
        undoStack.addLast(command)
    }

    fun clear() { undoStack.clear(); redoStack.clear() }
}
