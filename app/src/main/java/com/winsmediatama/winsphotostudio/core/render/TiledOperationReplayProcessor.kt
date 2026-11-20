package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.os.CancellationSignal
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.CurvesProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.HslProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.LevelsProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.CubeLut
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import com.winsmediatama.winsphotostudio.core.importer.BitmapTilePlanner

/**
 * Replays pointwise/non-geometric edit operations from preserved source tiles.
 * Unsupported geometric or spatial operations are rejected explicitly.
 */
object TiledOperationReplayProcessor {
    data class OutputTile(val rect: android.graphics.Rect, val bitmap: Bitmap)

    fun supports(operations: List<EditOperation>): Boolean = operations.filter { it.enabled }.all {
        (it is EditOperation.BasicAdjust && it.values.clarity == 0f && it.values.sharpen == 0f) ||
            it is EditOperation.Levels || it is EditOperation.Curves || it is EditOperation.Hsl ||
            it is EditOperation.Lut
    }

    fun renderTile(
        source: Bitmap,
        rect: android.graphics.Rect,
        operations: List<EditOperation>,
        cancellation: CancellationSignal? = null
    ): OutputTile {
        require(!source.isRecycled && source.width > 0 && source.height > 0)
        require(rect.left >= 0 && rect.top >= 0 && rect.right <= source.width && rect.bottom <= source.height && !rect.isEmpty)
        require(supports(operations)) { "Operation stack contains spatial/geometric operations not yet tile-replayable" }
        cancellation?.throwIfCanceled()
        var current: Bitmap? = Bitmap.createBitmap(source, rect.left, rect.top, rect.width(), rect.height())
        try {
            operations.filter { it.enabled }.forEach { operation ->
                cancellation?.throwIfCanceled()
                val input = requireNotNull(current)
                val next = when (operation) {
                    is EditOperation.BasicAdjust -> BasicAdjustmentProcessor.apply(input, operation.values)
                    is EditOperation.Levels -> LevelsProcessor.apply(input, operation.values)
                    is EditOperation.Curves -> CurvesProcessor.apply(input, operation.values)
                    is EditOperation.Hsl -> HslProcessor.apply(input, operation.values)
                    is EditOperation.Lut -> CubeLut.parse(operation.cubeText).apply(input)
                    else -> error("Unsupported operation reached tiled replay")
                }
                if (next !== input && !input.isRecycled) input.recycle()
                current = next
            }
            val result = OutputTile(android.graphics.Rect(rect), requireNotNull(current))
            current = null
            return result
        } finally {
            current?.let { if (!it.isRecycled) it.recycle() }
        }
    }

    fun renderTiles(
        source: Bitmap,
        operations: List<EditOperation>,
        tileSize: Int = 512,
        cancellation: CancellationSignal? = null
    ): List<OutputTile> {
        require(!source.isRecycled && source.width > 0 && source.height > 0)
        val planned = BitmapTilePlanner.plan(source.width, source.height, tileSize)
        val output = mutableListOf<OutputTile>()
        try {
            planned.forEach { rect ->
                cancellation?.throwIfCanceled()
                output += renderTile(source, rect, operations, cancellation)
            }
            return output
        } catch (error: Throwable) {
            output.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
            throw error
        }
    }

    fun recycleTiles(tiles: List<OutputTile>) = tiles.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
}
