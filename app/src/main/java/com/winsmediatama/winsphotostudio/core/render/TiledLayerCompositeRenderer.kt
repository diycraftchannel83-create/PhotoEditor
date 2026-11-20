package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.CancellationSignal
import com.winsmediatama.winsphotostudio.core.editor.EditorBlendMode
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.editor.LayerMaskProcessor
import com.winsmediatama.winsphotostudio.core.importer.BitmapTilePlanner

/**
 * Bounded-memory tile compositor for simple bitmap-layer stacks.
 *
 * It deliberately rejects operation stacks, groups and clipping-to-below instead of silently
 * flattening incorrectly. Layer bitmaps are treated as canvas-aligned; each tile extracts the same
 * source rectangle from each layer, applies masks and opacity, then composites into a tile canvas.
 */
object TiledLayerCompositeRenderer {
    data class CompositeTile(val rect: Rect, val bitmap: Bitmap)

    /**
     * Composite and encode one scanline at a time. Only one horizontal tile (plus its masked
     * temporary) is allocated per layer at a time, so output memory does not scale with canvas area.
     * This is the preferred PNG path for supported flat bitmap stacks.
     */
    fun encodeStreamingPng(
        layers: List<EditorLayer>,
        width: Int,
        height: Int,
        output: java.io.OutputStream,
        tileWidth: Int = 512,
        cancellation: CancellationSignal? = null
    ) {
        require(width > 0 && height > 0 && tileWidth > 0)
        require(layers.isNotEmpty())
        require(layers.none { it.groupId != null || it.clipToBelow }) {
            "Streaming compositor currently supports flat layers without clipping"
        }
        require(layers.none { it.operations.isNotEmpty() }) {
            "Streaming compositor currently supports layers without operation stacks"
        }
        TiledPngEncoder.encodeRows(width, height, output, cancellation) { y, rowPixels ->
            var left = 0
            while (left < width) {
                cancellation?.throwIfCanceled()
                val segmentWidth = (width - left).coerceAtMost(tileWidth)
                val segmentRect = Rect(left, y, left + segmentWidth, y + 1)
                val composite = Bitmap.createBitmap(segmentWidth, 1, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(composite)
                    layers.filter { it.visible && !it.bitmap.isRecycled }.forEach { layer ->
                        cancellation?.throwIfCanceled()
                        val cropped = extractAlignedRegion(layer.bitmap, segmentRect)
                        var masked: Bitmap? = null
                        try {
                            masked = layer.mask?.takeUnless { it.isRecycled }?.let { mask ->
                                val maskRow = extractAlignedRegion(mask, segmentRect)
                                try { LayerMaskProcessor.apply(cropped, maskRow) }
                                finally { if (!maskRow.isRecycled) maskRow.recycle() }
                            }
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                                alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
                                xfermode = blendMode(layer.blendMode)
                            }
                            canvas.drawBitmap(masked ?: cropped, 0f, 0f, paint)
                            paint.xfermode = null
                        } finally {
                            masked?.let { if (!it.isRecycled) it.recycle() }
                            if (!cropped.isRecycled) cropped.recycle()
                        }
                    }
                    composite.getPixels(rowPixels, left, width, 0, 0, segmentWidth, 1)
                } finally {
                    if (!composite.isRecycled) composite.recycle()
                }
                left += segmentWidth
            }
        }
    }

    fun renderTiles(
        layers: List<EditorLayer>,
        width: Int,
        height: Int,
        tileSize: Int = 512,
        cancellation: CancellationSignal? = null
    ): List<CompositeTile> {
        require(width > 0 && height > 0) { "Canvas dimensions must be positive" }
        require(layers.isNotEmpty()) { "Layer stack must not be empty" }
        require(layers.none { it.groupId != null || it.clipToBelow }) {
            "Tile compositor currently supports flat layers without clipping"
        }
        require(layers.none { it.operations.isNotEmpty() }) {
            "Tile compositor currently supports bitmap layers without replayable operations"
        }
        val planned = BitmapTilePlanner.plan(width, height, tileSize)
        val outputs = mutableListOf<CompositeTile>()
        try {
            planned.forEach { rect ->
                cancellation?.throwIfCanceled()
                val tile = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(tile)
                    layers.filter { it.visible && !it.bitmap.isRecycled }.forEach { layer ->
                        cancellation?.throwIfCanceled()
                        val sourceRect = Rect(rect.left, rect.top, rect.right, rect.bottom)
                        val cropped = extractAlignedRegion(layer.bitmap, sourceRect)
                        var masked: Bitmap? = null
                        try {
                            masked = layer.mask?.takeUnless { it.isRecycled }?.let { mask ->
                                extractAlignedRegion(mask, sourceRect).let { maskTile ->
                                    try { LayerMaskProcessor.apply(cropped, maskTile) }
                                    finally { if (!maskTile.isRecycled) maskTile.recycle() }
                                }
                            }
                            val drawSource = masked ?: cropped
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                                alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
                                xfermode = blendMode(layer.blendMode)
                            }
                            canvas.drawBitmap(drawSource, 0f, 0f, paint)
                            paint.xfermode = null
                        } finally {
                            masked?.let { if (!it.isRecycled) it.recycle() }
                            if (!cropped.isRecycled) cropped.recycle()
                        }
                    }
                    cancellation?.throwIfCanceled()
                    outputs += CompositeTile(Rect(rect), tile)
                } catch (error: Throwable) {
                    if (!tile.isRecycled) tile.recycle()
                    throw error
                }
            }
            cancellation?.throwIfCanceled()
            return outputs
        } catch (error: Throwable) {
            outputs.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
            throw error
        }
    }

    fun stitch(
        tiles: List<CompositeTile>,
        width: Int,
        height: Int,
        maxPixels: Long = 24_000_000L,
        cancellation: CancellationSignal? = null
    ): Bitmap {
        require(width > 0 && height > 0 && width.toLong() * height <= maxPixels) {
            "Flattened composite exceeds configured pixel safety limit"
        }
        cancellation?.throwIfCanceled()
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            tiles.forEach { tile ->
                cancellation?.throwIfCanceled()
                canvas.drawBitmap(tile.bitmap, tile.rect.left.toFloat(), tile.rect.top.toFloat(), null)
            }
            cancellation?.throwIfCanceled()
            return output
        } catch (error: Throwable) {
            if (!output.isRecycled) output.recycle()
            throw error
        }
    }

    fun recycleTiles(tiles: List<CompositeTile>) {
        tiles.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
    }

    private fun extractAlignedRegion(source: Bitmap, rect: Rect): Bitmap {
        val output = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            val sourceRect = Rect(
                rect.left.coerceIn(0, source.width),
                rect.top.coerceIn(0, source.height),
                rect.right.coerceIn(0, source.width),
                rect.bottom.coerceIn(0, source.height)
            )
            if (!sourceRect.isEmpty) {
                val destRect = Rect(
                    sourceRect.left - rect.left,
                    sourceRect.top - rect.top,
                    sourceRect.right - rect.left,
                    sourceRect.bottom - rect.top
                )
                canvas.drawBitmap(source, sourceRect, destRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            }
            return output
        } catch (error: Throwable) {
            if (!output.isRecycled) output.recycle()
            throw error
        }
    }

    private fun blendMode(mode: EditorBlendMode) = android.graphics.PorterDuffXfermode(
        when (mode) {
            EditorBlendMode.NORMAL -> android.graphics.PorterDuff.Mode.SRC_OVER
            EditorBlendMode.MULTIPLY -> android.graphics.PorterDuff.Mode.MULTIPLY
            EditorBlendMode.SCREEN -> android.graphics.PorterDuff.Mode.SCREEN
            EditorBlendMode.OVERLAY -> android.graphics.PorterDuff.Mode.OVERLAY
            EditorBlendMode.DARKEN -> android.graphics.PorterDuff.Mode.DARKEN
            EditorBlendMode.LIGHTEN -> android.graphics.PorterDuff.Mode.LIGHTEN
            EditorBlendMode.ADD -> android.graphics.PorterDuff.Mode.ADD
            EditorBlendMode.DIFFERENCE -> android.graphics.PorterDuff.Mode.XOR
        }
    )
}
