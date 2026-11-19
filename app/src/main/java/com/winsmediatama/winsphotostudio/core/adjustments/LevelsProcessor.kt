package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import kotlin.math.pow

/** Levels reference renderer: composite input/output controls followed by per-channel controls. */
object LevelsProcessor {
    fun apply(source: Bitmap, levels: LevelsValues): Bitmap {
        require(levels.composite.isValid() && levels.red.isValid() && levels.green.isValid() && levels.blue.isValid()) { "Invalid Levels range" }
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val data = IntArray(source.width * source.height)
        source.getPixels(data, 0, source.width, 0, 0, source.width, source.height)
        for (i in data.indices) {
            val p = data[i]
            val alpha = p ushr 24
            val originalR = (p shr 16) and 0xFF
            val originalG = (p shr 8) and 0xFF
            val originalB = p and 0xFF
            val compositeR = map(originalR, levels.composite)
            val compositeG = map(originalG, levels.composite)
            val compositeB = map(originalB, levels.composite)
            val r = map(compositeR, levels.red)
            val g = map(compositeG, levels.green)
            val b = map(compositeB, levels.blue)
            data[i] = (alpha shl 24) or (r shl 16) or (g shl 8) or b
        }
        result.setPixels(data, 0, source.width, 0, 0, source.width, source.height)
        return result
    }

    private fun map(value: Int, levels: LevelsChannel): Int {
        val normalized = (value / 255f - levels.inputBlack) / (levels.inputWhite - levels.inputBlack)
        val clamped = normalized.coerceIn(0f, 1f)
        val gammaCorrected = clamped.pow(1f / levels.inputGamma)
        val output = levels.outputBlack + gammaCorrected * (levels.outputWhite - levels.outputBlack)
        return (output * 255f).toInt().coerceIn(0, 255)
    }
}
