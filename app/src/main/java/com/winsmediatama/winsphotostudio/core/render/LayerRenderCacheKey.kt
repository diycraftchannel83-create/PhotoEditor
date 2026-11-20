package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import java.security.MessageDigest

/** Deterministic cache key; pixel-content hashes supplement generation IDs for reliable invalidation. */
object LayerRenderCacheKey {
    private const val VERSION = "winsphoto-layer-stack-v2"

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
        return VERSION + ":" + digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    /** Hashes pixels row-wise without allocating a full bitmap-sized buffer. */
    private fun bitmapToken(bitmap: Bitmap?): String {
        if (bitmap == null) return "null"
        if (bitmap.isRecycled) return "recycled"
        val pixelDigest = MessageDigest.getInstance("SHA-256")
        val row = IntArray(bitmap.width)
        val bytes = ByteArray(bitmap.width * 4)
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            var p = 0
            for (color in row) {
                bytes[p++] = (color ushr 24).toByte()
                bytes[p++] = (color ushr 16).toByte()
                bytes[p++] = (color ushr 8).toByte()
                bytes[p++] = color.toByte()
            }
            pixelDigest.update(bytes)
        }
        val content = pixelDigest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return bitmap.width.toString() + "x" + bitmap.height + "@" + bitmap.generationId + ":" + bitmap.config + ":" + content
    }
}
