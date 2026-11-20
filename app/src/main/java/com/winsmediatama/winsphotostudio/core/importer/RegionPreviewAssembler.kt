package com.winsmediatama.winsphotostudio.core.importer

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.CancellationSignal
import kotlin.math.ceil

/**
 * Builds a bounded preview by decoding source regions rather than allocating a full-resolution bitmap.
 * The result is preview-sized only; it is not a substitute for source-resolution export replay.
 */
object RegionPreviewAssembler {
    const val DEFAULT_TILE_SIZE = 1024
    const val MAX_OUTPUT_PIXELS = 4_194_304L

    data class OutputSize(val width: Int, val height: Int, val sampleSize: Int)

    fun calculateOutputSize(sourceWidth: Int, sourceHeight: Int, maxDimension: Int): OutputSize {
        require(sourceWidth in 1..PreviewImageDecoder.MAX_SOURCE_DIMENSION)
        require(sourceHeight in 1..PreviewImageDecoder.MAX_SOURCE_DIMENSION)
        require(maxDimension in 1..PreviewImageDecoder.MAX_PREVIEW_DIMENSION)
        val sample = PreviewImageDecoder.calculateSampleSize(sourceWidth, sourceHeight, maxDimension)
        val width = ceil(sourceWidth.toDouble() / sample).toInt().coerceAtLeast(1)
        val height = ceil(sourceHeight.toDouble() / sample).toInt().coerceAtLeast(1)
        require(width.toLong() * height <= MAX_OUTPUT_PIXELS) { "Preview output exceeds pixel safety limit" }
        return OutputSize(width, height, sample)
    }

    fun decode(
        resolver: ContentResolver,
        uri: Uri,
        maxDimension: Int = PreviewImageDecoder.MAX_PREVIEW_DIMENSION,
        tileSize: Int = DEFAULT_TILE_SIZE,
        cancellation: CancellationSignal? = null
    ): DecodedPreview? {
        require(maxDimension in 1..PreviewImageDecoder.MAX_PREVIEW_DIMENSION)
        require(tileSize in 128..2048)
        cancellation?.throwIfCanceled()
        val decoder = BitmapRegionTileDecoder.open(resolver, uri) ?: return null
        decoder.use {
            val width = it.sourceWidth
            val height = it.sourceHeight
            val size = calculateOutputSize(width, height, maxDimension)
            val sample = size.sampleSize
            val outputWidth = size.width
            val outputHeight = size.height
            val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
            try {
                var top = 0
                while (top < height) {
                    cancellation?.throwIfCanceled()
                    val bottom = (top + tileSize).coerceAtMost(height)
                    var left = 0
                    while (left < width) {
                        cancellation?.throwIfCanceled()
                        val right = (left + tileSize).coerceAtMost(width)
                        val sourceRect = Rect(left, top, right, bottom)
                        val tile = it.decode(sourceRect, sample, cancellation)
                        try {
                            val dst = Rect(
                                left / sample,
                                top / sample,
                                ceil(right.toDouble() / sample).toInt(),
                                ceil(bottom.toDouble() / sample).toInt()
                            )
                            canvas.drawBitmap(tile, null, dst, paint)
                        } finally {
                            if (!tile.isRecycled) tile.recycle()
                        }
                        left = right
                    }
                    top = bottom
                }
                cancellation?.throwIfCanceled()
                return DecodedPreview(output, width, height, sample)
            } catch (error: Throwable) {
                if (!output.isRecycled) output.recycle()
                throw error
            }
        }
    }
}
