package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.os.CancellationSignal
import android.graphics.Rect
import android.graphics.Canvas
import com.winsmediatama.winsphotostudio.core.adjustments.PhotoEffectKind
import com.winsmediatama.winsphotostudio.core.adjustments.EffectProcessor
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

    fun haloRadius(operations: List<EditOperation>): Int? {
        var radius = 0
        operations.filter { it.enabled }.forEach { operation ->
            when (operation) {
                is EditOperation.BasicAdjust -> if (operation.values.sharpen > 0f) radius += 1
                is EditOperation.Effect -> when (operation.settings.kind) {
                    PhotoEffectKind.BLUR -> radius += (operation.settings.strength * 24).toInt().coerceIn(0,24)
                    PhotoEffectKind.MOTION -> radius += if (operation.settings.strength <= 0f) 0 else (operation.settings.strength * 24).toInt().coerceIn(1,24)
                    PhotoEffectKind.VIGNETTE, PhotoEffectKind.GRAIN -> return null
                }
                is EditOperation.Levels, is EditOperation.Curves, is EditOperation.Hsl, is EditOperation.Lut -> Unit
                else -> return null
            }
        }
        return radius
    }

    fun supportsHalo(operations: List<EditOperation>): Boolean = haloRadius(operations) != null

    fun supports(operations: List<EditOperation>): Boolean = operations.filter { it.enabled }.all {
        (it is EditOperation.BasicAdjust && it.values.clarity == 0f && it.values.sharpen == 0f) ||
            it is EditOperation.Levels || it is EditOperation.Curves || it is EditOperation.Hsl ||
            it is EditOperation.Lut
    }

    fun renderHaloTile(
        source: Bitmap,
        coreRect: Rect,
        operations: List<EditOperation>,
        cancellation: CancellationSignal? = null
    ): OutputTile {
        val halo = requireNotNull(haloRadius(operations)) { "Operation stack is not supported by halo replay" }
        require(coreRect.left >= 0 && coreRect.top >= 0 && coreRect.right <= source.width && coreRect.bottom <= source.height && !coreRect.isEmpty)
        val expanded = Rect(
            (coreRect.left - halo).coerceAtLeast(0),
            (coreRect.top - halo).coerceAtLeast(0),
            (coreRect.right + halo).coerceAtMost(source.width),
            (coreRect.bottom + halo).coerceAtMost(source.height)
        )
        cancellation?.throwIfCanceled()
        var current: Bitmap? = if (expanded.left == 0 && expanded.top == 0 && expanded.right == source.width && expanded.bottom == source.height) source.copy(Bitmap.Config.ARGB_8888, true) else Bitmap.createBitmap(source, expanded.left, expanded.top, expanded.width(), expanded.height())
        var cropped: Bitmap? = null
        try {
            operations.filter { it.enabled }.forEach { operation ->
                cancellation?.throwIfCanceled()
                val input = requireNotNull(current)
                val next = when (operation) {
                    is EditOperation.BasicAdjust -> BasicAdjustmentProcessor.apply(input, operation.values, cancellation)
                    is EditOperation.Levels -> LevelsProcessor.apply(input, operation.values)
                    is EditOperation.Curves -> CurvesProcessor.apply(input, operation.values)
                    is EditOperation.Hsl -> HslProcessor.apply(input, operation.values)
                    is EditOperation.Lut -> CubeLut.parse(operation.cubeText).apply(input)
                    is EditOperation.Effect -> when (operation.settings.kind) {
                        PhotoEffectKind.BLUR -> EffectProcessor.gaussianBlur(input, (operation.settings.strength * 24).toInt(), cancellation)
                        PhotoEffectKind.MOTION -> if (operation.settings.strength <= 0f) input.copy(Bitmap.Config.ARGB_8888, true)
                            else EffectProcessor.motionBlur(input, (operation.settings.strength * 24).toInt().coerceAtLeast(1), operation.settings.angleDegrees, cancellation)
                        else -> error("Unsupported effect in halo replay")
                    }
                    else -> error("Unsupported operation reached halo replay")
                }
                if (next !== input && !input.isRecycled) input.recycle()
                current = next
            }
            val coreInExpanded = Rect(
                coreRect.left - expanded.left, coreRect.top - expanded.top,
                coreRect.right - expanded.left, coreRect.bottom - expanded.top
            )
            cropped = Bitmap.createBitmap(coreRect.width(), coreRect.height(), Bitmap.Config.ARGB_8888)
            Canvas(cropped).drawBitmap(requireNotNull(current), coreInExpanded, Rect(0, 0, coreRect.width(), coreRect.height()), null)
            cancellation?.throwIfCanceled()
            val result = OutputTile(Rect(coreRect), requireNotNull(cropped))
            cropped = null
            return result
        } finally {
            current?.let { if (!it.isRecycled) it.recycle() }
            cropped?.let { if (!it.isRecycled) it.recycle() }
        }
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
        var current: Bitmap? = if (rect.left == 0 && rect.top == 0 && rect.right == source.width && rect.bottom == source.height) source.copy(Bitmap.Config.ARGB_8888, true) else Bitmap.createBitmap(source, rect.left, rect.top, rect.width(), rect.height())
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
