package com.winsmediatama.winsphotudio.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.CancellationSignal

/**
 * Halo/overlap-aware tile processor for neighborhood-dependent operations (blur, edge filters, etc.).
 *
 * Each output tile covers a non-overlapping core rectangle. The processor receives an expanded source
 * rectangle including the requested halo, clamped to image bounds. Its output must preserve expanded
 * dimensions; only the core is cropped and returned. This avoids seams when the halo is large enough
 * for the processor's neighborhood radius.
 */
object BitmapHaloTileProcessor {
    data class HaloTile(
        val coreRect: Rect,
        val expandedRect: Rect,
        val bitmap: Bitmap
    )

    fun process(
        source: Bitmap,
        tileSize: Int = BitmapTilePlanner.DEFAULT_TILE_SIZE,
        halo: Int,
        cancellation: CancellationSignal? = null,
        processor: (Bitmap, Rect, CancellationSignal?) -> Bitmap
    ): List<HaloTile> {
        require(halo >= 0) { "Halo must not be negative" }
        require(!source.isRecycled) { "Source bitmap is recycled" }
        val cores = BitmapTilePlanner.plan(source.width, source.height, tileSize)
        val results = mutableListOf<HaloTile>()
        try {
            cores.forEach { tile ->
                val core = tile.sourceRect
                cancellation?.throwIfCanceled()
                val expanded = Rect(
                    (core.left - halo).coerceAtLeast(0),
                    (core.top - halo).coerceAtLeast(0),
                    (core.right + halo).coerceAtMost(source.width),
                    (core.bottom + halo).coerceAtMost(source.height)
                )
                val input = Bitmap.createBitmap(source, expanded.left, expanded.top, expanded.width(), expanded.height())
                var processed: Bitmap? = null
                var cropped: Bitmap? = null
                try {
                    processed = processor(input, Rect(expanded), cancellation)
                    require(!processed.isRecycled) { "Processor returned a recycled bitmap" }
                    require(processed.width == expanded.width() && processed.height == expanded.height()) {
                        "Halo processor must preserve expanded region dimensions"
                    }
                    cancellation?.throwIfCanceled()
                    val coreInExpanded = Rect(
                        core.left - expanded.left,
                        core.top - expanded.top,
                        core.right - expanded.left,
                        core.bottom - expanded.top
                    )
                    cropped = Bitmap.createBitmap(core.width(), core.height(), Bitmap.Config.ARGB_8888)
                    Canvas(cropped).drawBitmap(processed, coreInExpanded, Rect(0, 0, core.width(), core.height()), null)
                    cancellation?.throwIfCanceled()
                    results += HaloTile(Rect(core), Rect(expanded), cropped)
                    cropped = null // ownership transfers to result
                } finally {
                    if (!input.isRecycled) input.recycle()
                    processed?.let { if (!it.isRecycled) it.recycle() }
                    cropped?.let { if (!it.isRecycled) it.recycle() }
                }
            }
            cancellation?.throwIfCanceled()
            return results
        } catch (error: Throwable) {
            results.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
            throw error
        }
    }

    fun stitch(
        tiles: List<HaloTile>,
        width: Int,
        height: Int,
        maxPixels: Long = 24_000_000L,
        cancellation: CancellationSignal? = null
    ): Bitmap {
        require(width > 0 && height > 0 && width.toLong() * height <= maxPixels) {
            "Stitched bitmap exceeds configured pixel safety limit"
        }
        cancellation?.throwIfCanceled()
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            tiles.forEach { tile ->
                cancellation?.throwIfCanceled()
                require(tile.bitmap.width == tile.coreRect.width() && tile.bitmap.height == tile.coreRect.height()) {
                    "Tile bitmap dimensions do not match core geometry"
                }
                canvas.drawBitmap(tile.bitmap, tile.coreRect.left.toFloat(), tile.coreRect.top.toFloat(), null)
            }
            cancellation?.throwIfCanceled()
            return output
        } catch (error: Throwable) {
            if (!output.isRecycled) output.recycle()
            throw error
        }
    }

    fun recycleTiles(tiles: List<HaloTile>) {
        tiles.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
    }
}
