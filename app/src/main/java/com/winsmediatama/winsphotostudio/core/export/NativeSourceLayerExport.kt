package com.winsmediatama.winsphotostudio.core.export

import android.graphics.Bitmap
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.editor.LayerCompositeRenderer
import com.winsmediatama.winsphotostudio.core.history.FullResolutionEditReplay

/**
 * Reconstructs a canvas at source resolution by replaying the imported source layer,
 * then compositing the remaining editor layers at the reconstructed canvas size.
 * Only the identified source layer can be replayed natively; destructive source edits
 * invalidate this route and should cause the caller to use preview-based export.
 */
object NativeSourceLayerExport {
    fun render(
        source: Bitmap,
        layers: List<EditorLayer>,
        sourceLayerId: String,
        previewWidth: Int,
        previewHeight: Int
    ): Bitmap? {
        require(!source.isRecycled)
        require(source.width.toLong() * source.height <= FullResolutionExport.MAX_EXPORT_PIXELS)
        val sourceLayer = layers.firstOrNull { it.id == sourceLayerId } ?: return null
        if (sourceLayer.originalBitmap?.isRecycled != false) return null
        if (sourceLayer.originalBitmap == null) return null
        val nativeSource = if (sourceLayer.operations.isNotEmpty()) {
            FullResolutionEditReplay.render(source, sourceLayer.operations, previewWidth, previewHeight)
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }
        val nativeLayers = layers.map { layer ->
            if (layer.id == sourceLayerId) layer.copy(bitmap = nativeSource, originalBitmap = null, operations = emptyList())
            else layer
        }
        return try {
            LayerCompositeRenderer.render(nativeLayers, nativeSource.width, nativeSource.height)
        } finally {
            nativeSource.recycle()
        }
    }
}
