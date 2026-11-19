package com.winsmediatama.winsphotostudio.core.history

import org.junit.Assert.*
import org.junit.Test

class CommandHistoryTest {
    @Test fun executeUndoRedoAndClearRedoOnNewCommand() {
        var value=0
        val history=CommandHistory(maxEntries=3)
        fun command(label:String,delta:Int)=object:EditCommand {
            override val label=label
            override fun apply(){value+=delta}
            override fun undo(){value-=delta}
        }
        history.execute(command("Add 2",2))
        history.execute(command("Add 3",3))
        assertEquals(5,value);assertTrue(history.canUndo);assertEquals("Add 3",history.undoLabel)
        history.undo();assertEquals(2,value);assertTrue(history.canRedo)
        history.redo();assertEquals(5,value);assertFalse(history.canRedo)
        history.undo();history.execute(command("Add 7",7))
        assertEquals(9,value);assertFalse(history.canRedo)
    }
    @Test fun historyIsBounded() {
        var value=0
        val history=CommandHistory(maxEntries=2)
        repeat(3){n->history.execute(object:EditCommand{override val label="n$n";override fun apply(){value++};override fun undo(){value--}})}
        assertEquals(3,value);history.undo();history.undo();assertEquals(1,value);assertFalse(history.canUndo)
    }
}
