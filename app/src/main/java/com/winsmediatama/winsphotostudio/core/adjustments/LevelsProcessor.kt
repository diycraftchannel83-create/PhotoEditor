package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import android.os.CancellationSignal
import kotlin.math.pow

/** Levels reference renderer: composite input/output controls followed by per-channel controls. */
object LevelsProcessor {
    fun apply(source: Bitmap, levels: LevelsValues, cancellation: CancellationSignal? = null): Bitmap {
        require(levels.composite.isValid() && levels.red.isValid() && levels.green.isValid() && levels.blue.isValid()) { "Invalid Levels range" }
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val data = IntArray(width * height)
            source.getPixels(data, 0, width, 0, 0, width, height)
            for (y in 0 until height) {
                cancellation?.throwIfCanceled()
                val start = y * width
                for (i in start until start + width) {
                    val p = data[i]
                    val alpha = p ushr 24
                    val compositeR = map((p shr 16) and 0xFF, levels.composite)
                    val compositeG = map((p shr 8) and 0xFF, levels.composite)
                    val compositeB = map(p and 0xFF, levels.composite)
                    val r = map(compositeR, levels.red)
                    val g = map(compositeG, levels.green)
                    val b = map(compositeB, levels.blue)
                    data[i] = (alpha shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            cancellation?.throwIfCanceled()
            result.setPixels(data, 0, width, 0, 0, width, height)
            cancellation?.throwIfCanceled()
            return result
        } catch (error: Throwable) {
            if (!result.isRecycled) result.recycle()
            throw error
        }
    }

    private fun map(value: Int, levels: LevelsChannel): Int {
        val normalized = (value / 255f - levels.inputBlack) / (levels.inputWhite - levels.inputBlack)
        val clamped = normalized.coerceIn(0f, 1f)
        val gammaCorrected = clamped.pow(1f / levels.inputGamma)
        val output = levels.outputBlack + gammaCorrected * (levels.outputWhite - levels.outputBlack)
        return (output * 255f).toInt().coerceIn(0, 255)
    }
}
