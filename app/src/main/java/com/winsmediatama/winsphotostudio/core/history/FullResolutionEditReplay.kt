package com.winsmediatama.winsphotostudio.core.history

import android.graphics.Bitmap

/**
 * Scales dimension-bound edit parameters from a preview-space operation stack to a decoded
 * source bitmap. Normalized geometry and brush coordinates remain unchanged.
 *
 * This is a replay foundation, not an export integration: the caller must provide the
 * source bitmap and original preview dimensions used when the edits were authored.
 */
object FullResolutionEditReplay {
    data class ReplayDimensions(
        val previewWidth: Int,
        val previewHeight: Int,
        val sourceWidth: Int,
        val sourceHeight: Int
    ) {
        init {
            require(previewWidth > 0 && previewHeight > 0 && sourceWidth > 0 && sourceHeight > 0)
        }
        val scaleX: Float get() = sourceWidth.toFloat() / previewWidth
        val scaleY: Float get() = sourceHeight.toFloat() / previewHeight
    }

    fun scaleOperations(
        operations: List<EditOperation>,
        dimensions: ReplayDimensions
    ): List<EditOperation> = operations.map { operation ->
        when (operation) {
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
    }

    /**
     * Replay edits against a source bitmap using source-space output dimensions.
     * The source is never mutated or recycled by this method.
     */
    fun render(
        source: Bitmap,
        operations: List<EditOperation>,
        previewWidth: Int,
        previewHeight: Int
    ): Bitmap {
        require(!source.isRecycled && source.width > 0 && source.height > 0)
        val dimensions = ReplayDimensions(previewWidth, previewHeight, source.width, source.height)
        val scaled = scaleOperations(operations, dimensions)
        return NonDestructiveEditStack(source, scaled).render()
    }

    private fun scaleDimension(value: Int, scale: Float): Int {
        if (value <= 0) return value
        require(scale.isFinite() && scale > 0f)
        val scaled = value.toDouble() * scale.toDouble()
        require(scaled <= 16384.0) { "Scaled edit dimension exceeds 16384px safety limit" }
        return scaled.roundToIntSafe().coerceAtLeast(1)
    }

    private fun Double.roundToIntSafe(): Int = kotlin.math.floor(this + 0.5).toInt()
}
