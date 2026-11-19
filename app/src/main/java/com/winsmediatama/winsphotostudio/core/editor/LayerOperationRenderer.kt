package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack

/** Utilities for rendering an editor layer's replayable adjustments without modifying its source. */
object LayerOperationRenderer {
    /**
     * Renders operation metadata from the preserved original. If no replayable source/operations
     * exist, returns a copy of the current layer bitmap. Returned bitmap is caller-owned.
     */
    fun render(layer: EditorLayer): Bitmap {
        val source = layer.originalBitmap?.takeUnless { it.isRecycled }
        if (source != null && layer.operations.isNotEmpty()) {
            return NonDestructiveEditStack(source, layer.operations).render()
        }
        return layer.bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    /** True when this layer can be reconstructed from source plus operation parameters. */
    fun isReplayable(layer: EditorLayer): Boolean =
        layer.originalBitmap?.let { !it.isRecycled } == true && layer.operations.isNotEmpty()
}
