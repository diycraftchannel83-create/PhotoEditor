package com.winsmediatama.winsphotostudio.core.history

import android.graphics.Bitmap
import org.json.JSONArray

/**
 * Stateful controller for a non-destructive edit session.
 * Keeps a private copy of the imported source and operation-JSON snapshots for undo/redo.
 * Rendering always starts from the source; edits never mutate the source bitmap.
 */
class NonDestructiveEditorSession(source: Bitmap, initialOperations: JSONArray = JSONArray(), private val historyLimit: Int = 30) {
    private val original: Bitmap = source.copy(Bitmap.Config.ARGB_8888, false)
    private var stack = NonDestructiveEditStack(original, NonDestructiveEditStack.fromJson(original, initialOperations).operations)
    private val undoStates = ArrayDeque<JSONArray>()
    private val redoStates = ArrayDeque<JSONArray>()

    init { require(historyLimit >= 1) { "historyLimit must be at least 1" } }

    val operations: List<EditOperation> get() = stack.operations
    val canUndo: Boolean get() = undoStates.isNotEmpty()
    val canRedo: Boolean get() = redoStates.isNotEmpty()

    /** Returns a caller-owned bitmap. Caller should recycle it when no longer needed. */
    fun renderPreview(): Bitmap = stack.render()

    fun append(operation: EditOperation) { checkpoint(); stack.append(operation) }
    fun insert(index: Int, operation: EditOperation) { checkpoint(); stack.insert(index, operation) }
    fun move(id: String, index: Int): Boolean { if (operations.none { it.id == id }) return false; checkpoint(); return stack.move(id, index) }
    fun replace(id: String, operation: EditOperation): Boolean { if (operations.none { it.id == id }) return false; checkpoint(); return stack.replace(id, operation) }
    fun setEnabled(id: String, enabled: Boolean): Boolean { if (operations.none { it.id == id }) return false; checkpoint(); return stack.toggle(id, enabled) }
    fun remove(id: String): Boolean { if (operations.none { it.id == id }) return false; checkpoint(); return stack.remove(id) }
    fun clear() { if (operations.isNotEmpty()) { checkpoint(); stack.clear() } }

    fun undo(): Boolean {
        if (undoStates.isEmpty()) return false
        redoStates.addLast(stack.toJson())
        stack = restore(undoStates.removeLast())
        return true
    }

    fun redo(): Boolean {
        if (redoStates.isEmpty()) return false
        pushBounded(undoStates, stack.toJson())
        stack = restore(redoStates.removeLast())
        return true
    }

    fun exportOperations(): JSONArray = stack.toJson()
    fun renderFinal(): Bitmap = renderPreview()

    private fun checkpoint() {
        pushBounded(undoStates, stack.toJson())
        redoStates.clear()
    }

    private fun restore(json: JSONArray): NonDestructiveEditStack = NonDestructiveEditStack.fromJson(original, json)
    private fun pushBounded(queue: ArrayDeque<JSONArray>, state: JSONArray) {
        queue.addLast(JSONArray(state.toString()))
        while (queue.size > historyLimit) queue.removeFirst()
    }
}
