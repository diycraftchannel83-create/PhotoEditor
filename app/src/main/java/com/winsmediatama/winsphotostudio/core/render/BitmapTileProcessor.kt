package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.CancellationSignal

/**
 * Bounded source-region rendering primitive. A processor receives a single source tile and must
 * return a tile with identical dimensions; ownership of each returned bitmap transfers to the caller.
 * The source bitmap remains caller-owned and is never recycled.
 */
object BitmapTileProcessor {
    data class ProcessedTile(val sourceRect: Rect, val bitmap: Bitmap)

    fun process(
        source: Bitmap,
        tileSize: Int = BitmapTilePlanner.DEFAULT_TILE_SIZE,
        cancellation: CancellationSignal? = null,
        processor: (Bitmap, Rect, CancellationSignal?) -> Bitmap
    ): List<ProcessedTile> {
        require(!source.isRecycled) { "Source bitmap is recycled" }
        val planner = BitmapTilePlanner.plan(source.width, source.height, tileSize)
        val output = mutableListOf<ProcessedTile>()
        try {
            planner.forEach { rect ->
                cancellation?.throwIfCanceled()
                val inputTile = Bitmap.createBitmap(source, rect.left, rect.top, rect.width(), rect.height())
                var processed: Bitmap? = null
                try {
                    processed = processor(inputTile, Rect(rect), cancellation)
                    require(!processed.isRecycled) { "Tile processor returned a recycled bitmap" }
                    require(processed.width == rect.width() && processed.height == rect.height()) {
                        "Tile processor must preserve tile dimensions"
                    }
                    cancellation?.throwIfCanceled()
                    output += ProcessedTile(Rect(rect), processed)
                    processed = null // ownership transfers to returned list
                } finally {
                    if (!inputTile.isRecycled) inputTile.recycle()
                    processed?.let { if (!it.isRecycled) it.recycle() }
                }
            }
            cancellation?.throwIfCanceled()
            return output
        } catch (error: Throwable) {
            output.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
            throw error
        }
    }

    /** Stitch tiles into a bounded canvas. Callers must enforce their desired output pixel ceiling. */
    fun stitch(
        tiles: List<ProcessedTile>,
        width: Int,
        height: Int,
        maxPixels: Long = 24_000_000L,
        cancellation: CancellationSignal? = null
    ): Bitmap {
        require(width > 0 && height > 0 && width.toLong() * height <= maxPixels) {
            "Stitched bitmap exceeds configured pixel safety limit"
        }
        cancellation?.throwIfCanceled()
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = android.graphics.Canvas(result)
            tiles.forEach { tile ->
                cancellation?.throwIfCanceled()
                canvas.drawBitmap(tile.bitmap, tile.sourceRect.left.toFloat(), tile.sourceRect.top.toFloat(), null)
            }
            cancellation?.throwIfCanceled()
            return result
        } catch (error: Throwable) {
            if (!result.isRecycled) result.recycle()
            throw error
        }
    }

    fun recycleTiles(tiles: List<ProcessedTile>) {
        tiles.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
    }
}
