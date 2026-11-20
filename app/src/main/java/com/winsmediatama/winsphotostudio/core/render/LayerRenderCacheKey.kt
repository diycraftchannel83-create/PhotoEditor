package com.winsmediatama.winsphotostudio.core.render

import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import java.security.MessageDigest

/**
 * Deterministic cache key for layer-stack rendering.
 * Bitmap generation IDs invalidate entries after in-place pixel edits; layer properties,
 * masks, source bitmaps and non-destructive operation payloads are also included.
 */
object LayerRenderCacheKey {
    private const val VERSION = "winsphoto-layer-stack-v1"

    fun create(layers: List<EditorLayer>, canvasWidth: Int, canvasHeight: Int): String {
        require(canvasWidth > 0 && canvasHeight > 0)
        val canonical = buildString {
            append(VERSION).append('|').append(canvasWidth).append('x').append(canvasHeight)
            layers.forEach { layer ->
                append("|layer:").append(layer.id)
                append("|visible:").append(layer.visible)
                append("|opacity:").append(layer.opacity)
                append("|blend:").append(layer.blendMode)
                append("|group:").append(layer.groupId).append(':').append(layer.groupOpacity).append(':').append(layer.groupBlendMode)
                append("|clip:").append(layer.clipToBelow)
                append("|locked:").append(layer.locked).append(':').append(layer.alphaLocked)
                append("|bitmap:").append(bitmapToken(layer.bitmap))
                append("|mask:").append(bitmapToken(layer.mask))
                append("|original:").append(bitmapToken(layer.originalBitmap))
                append("|ops:")
                layer.operations.forEach { operation -> append(operation.toString()).append(';') }
            }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return VERSION + ":" + digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun bitmapToken(bitmap: android.graphics.Bitmap?): String {
        if (bitmap == null) return "null"
        if (bitmap.isRecycled) return "recycled:" + bitmap.width + "x" + bitmap.height
        return bitmap.width.toString() + "x" + bitmap.height + "@" + bitmap.generationId + ":" + bitmap.config
    }
}
