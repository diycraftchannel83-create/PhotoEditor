package com.winsmediatama.winsphotostudio.core.history

import android.graphics.Bitmap

/**
 * Scales dimension-bound edit parameters from preview-space to source-space.
 * Normalized geometry and brush coordinates remain unchanged.
 */
object FullResolutionEditReplay {
    const val MAX_REPLAY_PIXELS = 24_000_000L

    data class ReplayDimensions(
        val previewWidth: Int,
        val previewHeight: Int,
        val sourceWidth: Int,
        val sourceHeight: Int
    ) {
        init {
            require(previewWidth > 0 && previewHeight > 0 && sourceWidth > 0 && sourceHeight > 0)
            require(sourceWidth.toLong() * sourceHeight <= MAX_REPLAY_PIXELS) {
                "Source exceeds replay pixel safety limit"
            }
        }
        val scaleX: Float get() = sourceWidth.toFloat() / previewWidth
        val scaleY: Float get() = sourceHeight.toFloat() / previewHeight
    }

    fun scaleOperations(
        operations: List<EditOperation>,
        dimensions: ReplayDimensions
    ): List<EditOperation> = operations.map { operation ->
        val scaled = when (operation) {
            is EditOperation.Transform -> operation.copy(
                outputWidth = scaleDimension(operation.outputWidth, dimensions.scaleX),
                outputHeight = scaleDimension(operation.outputHeight, dimensions.scaleY)
            )
            is EditOperation.Perspective -> operation.copy(
                outputWidth = scaleDimension(operation.outputWidth, dimensions.scaleX),
                outputHeight = scaleDimension(operation.outputHeight, dimensions.scaleY)
            )
            is EditOperation.CanvasSize -> operation.copy(
                width = scaleDimension(operation.width, dimensions.scaleX).coerceAtLeast(1),
                height = scaleDimension(operation.height, dimensions.scaleY).coerceAtLeast(1)
            )
            else -> operation
        }
        when (scaled) {
            is EditOperation.Transform -> validatePixels(scaled.outputWidth, scaled.outputHeight)
            is EditOperation.Perspective -> validatePixels(scaled.outputWidth, scaled.outputHeight)
            is EditOperation.CanvasSize -> validatePixels(scaled.width, scaled.height)
            else -> Unit
        }
        scaled
    }

    /** Replay edits at source resolution. The source bitmap is never modified or recycled. */
    fun render(
        source: Bitmap,
        operations: List<EditOperation>,
        previewWidth: Int,
        previewHeight: Int
    ): Bitmap {
        require(!source.isRecycled && source.width > 0 && source.height > 0)
        require(source.width.toLong() * source.height <= MAX_REPLAY_PIXELS)
        val dimensions = ReplayDimensions(previewWidth, previewHeight, source.width, source.height)
        val scaled = scaleOperations(operations, dimensions)
        return NonDestructiveEditStack(source, scaled).render()
    }

    private fun validatePixels(width: Int, height: Int) {
        if (width > 0 && height > 0) require(width.toLong() * height <= MAX_REPLAY_PIXELS) {
            "Replay operation output exceeds 24 megapixel safety limit"
        }
    }

    private fun scaleDimension(value: Int, scale: Float): Int {
        if (value <= 0) return value
        require(scale.isFinite() && scale > 0f)
        val scaled = value.toDouble() * scale.toDouble()
        require(scaled <= 16384.0) { "Scaled edit dimension exceeds 16384px safety limit" }
        return kotlin.math.floor(scaled + 0.5).toInt().coerceAtLeast(1)
    }
}
