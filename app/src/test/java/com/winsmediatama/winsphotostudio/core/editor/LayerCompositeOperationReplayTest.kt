package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Color
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LayerCompositeOperationReplayTest {
    @Test
    fun compositeUsesReplayableOperationsFromOriginalInsteadOfStaleRenderedBitmap() {
        val original = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val staleRendered = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val layer = EditorLayer(name = "Adjusted", bitmap = staleRendered, originalBitmap = original, operations = listOf(
            EditOperation.Crop(enabled = false, left = 0f, top = 0f, right = 1f, bottom = 1f)
        ))
        val result = LayerCompositeRenderer.render(listOf(layer), 2, 2)
        assertEquals(Color.RED, result.getPixel(0, 0))
        assertEquals(Color.BLUE, staleRendered.getPixel(0, 0))
        assertEquals(Color.RED, original.getPixel(0, 0))
        result.recycle(); original.recycle(); staleRendered.recycle()
    }
}
