package com.winsmediatama.winsphotostudio.core.importer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.ContentResolver

/**
 * Bounds-first, memory-conscious image decoding for editor previews.
 * Keeps the original dimensions as metadata while only decoding a capped preview.
 */
data class DecodedPreview(val bitmap: Bitmap, val sourceWidth: Int, val sourceHeight: Int, val sampleSize: Int)

object PreviewImageDecoder {
    const val MAX_PREVIEW_DIMENSION = 2048
    const val MAX_SOURCE_DIMENSION = 100_000

    fun calculateSampleSize(width: Int, height: Int, maxDimension: Int = MAX_PREVIEW_DIMENSION): Int {
        if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
        var sample = 1
        while (width / sample > maxDimension || height / sample > maxDimension) {
            if (sample >= 1 shl 30) return sample
            sample *= 2
        }
        return sample
    }

    fun decode(resolver: ContentResolver, uri: Uri, maxDimension: Int = MAX_PREVIEW_DIMENSION): DecodedPreview? {
        require(maxDimension in 1..MAX_PREVIEW_DIMENSION) { "Preview size exceeds safety limit" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0 || width > MAX_SOURCE_DIMENSION || height > MAX_SOURCE_DIMENSION) return null
        val sample = calculateSampleSize(width, height, maxDimension)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inJustDecodeBounds = false
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        if (bitmap.width <= 0 || bitmap.height <= 0) {
            bitmap.recycle()
            return null
        }
        return DecodedPreview(bitmap, width, height, sample)
    }
}
