package com.winsmediatama.winsphotostudio.core.storage

import android.graphics.Bitmap
import android.graphics.Color
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProjectStoreOperationProjectTest {
    @Test
    fun operationProjectRoundTripPreservesOriginalAndStackParameters() {
        val context = RuntimeEnvironment.getApplication()
        val store = ProjectStore(context)
        val original = Bitmap.createBitmap(8, 6, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(120, 80, 40))
        }
        val stack = NonDestructiveEditStack(original).apply {
            append(EditOperation.BasicAdjust(values = BasicAdjustmentValues(brightness = 0.18f, contrast = 0.12f)))
            append(EditOperation.BasicAdjust(enabled = false, values = BasicAdjustmentValues(exposure = 0.7f)))
            append(EditOperation.Crop(left = 0.1f, top = 0.1f, right = 0.9f, bottom = 0.9f))
        }
        val operationIds = stack.operations.map { it.id }

        val project = store.saveOperationProject(original, stack.toJson(), "Non-destructive round trip")
        assertTrue(project.isDirectory)
        assertTrue(java.io.File(project, "operations.json").isFile)

        val loaded = store.loadOperationProject(project)
        assertNotNull(loaded)
        assertEquals("Non-destructive round trip", loaded!!.name)
        assertEquals(8, loaded.original.width)
        assertEquals(6, loaded.original.height)
        assertEquals(operationIds, loaded.editStack.operations.map { it.id })
        assertEquals(3, loaded.editStack.operations.size)
        assertTrue(loaded.editStack.operations[1].enabled.not())
        val first = loaded.editStack.operations[0] as EditOperation.BasicAdjust
        assertEquals(0.18f, first.values.brightness, 0.0001f)
        assertEquals(0.12f, first.values.contrast, 0.0001f)
        val third = loaded.editStack.operations[2] as EditOperation.Crop
        assertEquals(0.1f, third.left, 0.0001f)
        assertEquals(0.9f, third.right, 0.0001f)

        // Saving operations must not mutate the imported source pixels.
        assertEquals(Color.rgb(120, 80, 40), original.getPixel(0, 0))
        loaded.original.recycle()
        original.recycle()
    }

    @Test
    fun operationProjectCanReplayAfterLoading() {
        val context = RuntimeEnvironment.getApplication()
        val store = ProjectStore(context)
        val original = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val stack = NonDestructiveEditStack(original).apply {
            append(EditOperation.Crop(left = 0.2f, top = 0.2f, right = 0.8f, bottom = 0.8f))
        }
        val project = store.saveOperationProject(original, stack.toJson(), "Crop replay")
        val loaded = store.loadOperationProject(project)!!
        val replay = loaded.editStack.render()
        assertEquals(6, replay.width)
        assertEquals(6, replay.height)
        assertEquals(Color.BLUE, replay.getPixel(0, 0))
        replay.recycle()
        loaded.original.recycle()
        original.recycle()
    }
}
