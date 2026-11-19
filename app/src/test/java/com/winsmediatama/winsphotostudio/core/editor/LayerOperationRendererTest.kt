package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Color
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LayerOperationRendererTest {
    @Test
    fun replayableLayerRendersFromOriginalWithoutMutatingIt() {
        val original = Bitmap.createBitmap(6, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val current = Bitmap.createBitmap(6, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val layer = EditorLayer(name = "Test", bitmap = current, originalBitmap = original, operations = listOf(
            EditOperation.Crop(enabled = false, left = 0.25f, top = 0.25f, right = 0.75f, bottom = 0.75f)
        ))
        assertTrue(LayerOperationRenderer.isReplayable(layer))
        val rendered = LayerOperationRenderer.render(layer)
        assertEquals(6, rendered.width)
        assertEquals(4, rendered.height)
        assertEquals(Color.RED, rendered.getPixel(0, 0))
        assertEquals(Color.RED, original.getPixel(0, 0))
        assertEquals(Color.BLUE, current.getPixel(0, 0))
        rendered.recycle(); original.recycle(); current.recycle()
    }

    @Test
    fun nonReplayableLayerReturnsIndependentBitmapCopy() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val layer = EditorLayer(name = "Fallback", bitmap = bitmap)
        assertFalse(LayerOperationRenderer.isReplayable(layer))
        val rendered = LayerOperationRenderer.render(layer)
        assertNotSame(bitmap, rendered)
        assertEquals(Color.GREEN, rendered.getPixel(1, 1))
        rendered.recycle(); bitmap.recycle()
    }
}
