package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import kotlin.math.pow

/** CPU reference implementation for preview-sized bitmaps. The GPU pipeline will replace/accelerate this path. */
object BasicAdjustmentProcessor {
    fun apply(source: Bitmap, values: BasicAdjustmentValues): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        val contrastFactor = (1f + values.contrast.coerceIn(-1f, 1f)).coerceAtLeast(0f)
        val exposureFactor = 2.0.pow(values.exposure.coerceIn(-5f, 5f).toDouble()).toFloat()
        val saturationFactor = (1f + values.saturation.coerceIn(-1f, 1f)).coerceAtLeast(0f)
        val brightnessOffset = values.brightness.coerceIn(-1f, 1f) * 255f
        val temp = values.temperature.coerceIn(-1f, 1f) * 24f
        val tint = values.tint.coerceIn(-1f, 1f) * 18f
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = pixel ushr 24
            var r = ((pixel shr 16) and 0xFF).toFloat()
            var g = ((pixel shr 8) and 0xFF).toFloat()
            var b = (pixel and 0xFF).toFloat()
            r = ((r - 127.5f) * contrastFactor + 127.5f + brightnessOffset) * exposureFactor + temp
            g = ((g - 127.5f) * contrastFactor + 127.5f + brightnessOffset) * exposureFactor + tint
            b = ((b - 127.5f) * contrastFactor + 127.5f + brightnessOffset) * exposureFactor - temp
            val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
            r = luminance + (r - luminance) * saturationFactor
            g = luminance + (g - luminance) * saturationFactor
            b = luminance + (b - luminance) * saturationFactor
            pixels[i] = (a shl 24) or (r.toInt().coerceIn(0, 255) shl 16) or (g.toInt().coerceIn(0, 255) shl 8) or b.toInt().coerceIn(0, 255)
        }
        output.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        return output
    }
}
