package com.winsmediatama.winsphotostudio.core.history

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NonDestructiveEditorSessionTest {
    @Test
    fun appendUndoRedoAndExportKeepOriginalSourceUntouched() {
        val source = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }
        val session = NonDestructiveEditorSession(source, historyLimit = 2)
        val operation = EditOperation.Crop(left = 0.25f, top = 0.25f, right = 0.75f, bottom = 0.75f)
        session.append(operation)
        assertTrue(session.canUndo)
        assertFalse(session.canRedo)
        assertEquals(operation.id, session.operations.single().id)
        val preview = session.renderPreview()
        assertEquals(2, preview.width)
        assertEquals(1, preview.height)
        assertEquals(Color.MAGENTA, source.getPixel(0, 0))
        assertTrue(session.undo())
        assertTrue(session.operations.isEmpty())
        assertTrue(session.canRedo)
        assertTrue(session.redo())
        assertEquals(operation.id, session.operations.single().id)
        assertEquals(operation.id, session.exportOperations().getJSONObject(0).getString("id"))
        preview.recycle(); source.recycle()
    }

    @Test
    fun mutationAfterUndoClearsRedoBranch() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val session = NonDestructiveEditorSession(source)
        val first = EditOperation.Crop(left = 0f, top = 0f, right = 1f, bottom = 1f)
        val second = EditOperation.Crop(left = 0.1f, top = 0.1f, right = 0.9f, bottom = 0.9f)
        session.append(first)
        session.undo()
        session.append(second)
        assertFalse(session.canRedo)
        assertEquals(second.id, session.operations.single().id)
        source.recycle()
    }
}
