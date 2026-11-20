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
    @Test fun failedUndoKeepsCommandAvailable() {
        val history=CommandHistory()
        var state=1
        val command=object:EditCommand {
            override val label="fragile"
            override fun apply(){state++}
            override fun undo(){throw IllegalStateException("undo failed")}
        }
        history.execute(command)
        val failure=runCatching{history.undo()}.exceptionOrNull()
        assertNotNull(failure)
        assertTrue(history.canUndo);assertFalse(history.canRedo);assertEquals("fragile",history.undoLabel)
        assertEquals(2,state)
    }

    @Test fun failedRedoKeepsCommandOnRedoStack() {
        val history=CommandHistory()
        var failApply=false;var state=0
        val command=object:EditCommand {
            override val label="redo fragile"
            override fun apply(){if(failApply)throw IllegalStateException("apply failed");state++}
            override fun undo(){state--}
        }
        history.execute(command);history.undo();failApply=true
        val failure=runCatching{history.redo()}.exceptionOrNull()
        assertNotNull(failure)
        assertTrue(history.canRedo);assertFalse(history.canUndo);assertEquals("redo fragile",history.redoLabel)
        assertEquals(0,state)
    }

    @Test fun historyRejectsNonPositiveCapacity() {
        assertTrue(runCatching{CommandHistory(0)}.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching{CommandHistory(-1)}.exceptionOrNull() is IllegalArgumentException)
    }
}
