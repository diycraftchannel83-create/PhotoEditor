package com.winsmediatama.winsphotostudio.core.history

import android.graphics.Bitmap
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NonDestructiveEditStackOperationsTest {
    private fun source() = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

    @Test
    fun appendInsertAndMoveMaintainExpectedOrder() {
        val src = source()
        val a = EditOperation.BasicAdjust(values = BasicAdjustmentValues(brightness = .1f))
        val b = EditOperation.BasicAdjust(values = BasicAdjustmentValues(brightness = .2f))
        val c = EditOperation.BasicAdjust(values = BasicAdjustmentValues(brightness = .3f))
        val stack = NonDestructiveEditStack(src).apply {
            append(a)
            append(c)
            insert(1, b)
        }
        assertEquals(listOf(a.id, b.id, c.id), stack.operations.map { it.id })
        assertTrue(stack.move(c.id, 0))
        assertEquals(listOf(c.id, a.id, b.id), stack.operations.map { it.id })
        assertTrue(stack.move(c.id, 99))
        assertEquals(listOf(a.id, b.id, c.id), stack.operations.map { it.id })
        assertFalse(stack.move("unknown", 0))
    }

    @Test
    fun toggleReplaceAndRemoveOperateOnTargetId() {
        val src = source()
        val a = EditOperation.BasicAdjust(values = BasicAdjustmentValues(brightness = .1f))
        val b = EditOperation.BasicAdjust(values = BasicAdjustmentValues(brightness = .2f))
        val stack = NonDestructiveEditStack(src).apply { append(a); append(b) }

        assertTrue(stack.toggle(a.id, false))
        assertFalse(stack.operations.first().enabled)
        assertTrue(stack.operations.last().enabled)

        val replacement = a.copy(values = BasicAdjustmentValues(brightness = .5f))
        assertTrue(stack.replace(a.id, replacement))
        assertEquals(.5f, (stack.operations.first() as EditOperation.BasicAdjust).values.brightness, .0001f)
        assertTrue(stack.remove(a.id))
        assertEquals(listOf(b.id), stack.operations.map { it.id })
        assertFalse(stack.remove("missing"))
    }

    @Test
    fun clearRemovesAllOperationsAndRenderReturnsOriginalPixels() {
        val src = source()
        src.eraseColor(android.graphics.Color.GRAY)
        val stack = NonDestructiveEditStack(src).apply {
            append(EditOperation.BasicAdjust(values = BasicAdjustmentValues(exposure = 2f)))
            append(EditOperation.BasicAdjust(values = BasicAdjustmentValues(saturation = 1f)))
        }
        assertEquals(2, stack.operations.size)
        stack.clear()
        assertTrue(stack.operations.isEmpty())
        val output = stack.render()
        assertEquals(android.graphics.Color.GRAY, output.getPixel(0, 0))
    }

    @Test
    fun malformedCropOperationsAreSkippedDuringJsonLoad() {
        val src = source()
        val json = org.json.JSONArray().put(org.json.JSONObject()
            .put("id", "bad-crop")
            .put("type", "crop")
            .put("enabled", true)
            .put("left", 0.8).put("top", 0.2).put("right", 0.3).put("bottom", 0.9))
        val loaded = NonDestructiveEditStack.fromJson(src, json)
        assertTrue(loaded.operations.isEmpty())
    }
}
