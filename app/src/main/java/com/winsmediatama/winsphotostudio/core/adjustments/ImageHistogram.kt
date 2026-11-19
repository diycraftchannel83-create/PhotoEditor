package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import android.graphics.Color

/** Histogram for the original/preview image. Bins are 0..255 and alpha-zero pixels are ignored. */
data class ImageHistogram(
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray,
    val luminance: IntArray,
    val sampleCount: Int
) {
    companion object {
        fun from(bitmap: Bitmap): ImageHistogram {
            val red = IntArray(256)
            val green = IntArray(256)
            val blue = IntArray(256)
            val luminance = IntArray(256)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            var count = 0
            for (pixel in pixels) {
                if (Color.alpha(pixel) == 0) continue
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                red[r]++
                green[g]++
                blue[b]++
                luminance[((0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()).coerceIn(0, 255)]++
                count++
            }
            return ImageHistogram(red, green, blue, luminance, count)
        }
    }
}
