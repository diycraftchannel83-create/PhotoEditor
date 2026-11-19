package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Histogram for a source/preview image. Bins are 0..255 and fully transparent
 * pixels are ignored. Large images are sampled on a regular grid to keep the
 * operation bounded in both memory and CPU time.
 *
 * [sampleCount] is the number of visible pixels examined (not the total image
 * pixel count when sampling is active).
 */
data class ImageHistogram(
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray,
    val luminance: IntArray,
    val sampleCount: Int
) {
    companion object {
        const val MAX_SAMPLES: Int = 1_000_000

        fun from(bitmap: Bitmap): ImageHistogram {
            val red = IntArray(256)
            val green = IntArray(256)
            val blue = IntArray(256)
            val luminance = IntArray(256)
            if (bitmap.width <= 0 || bitmap.height <= 0) {
                return ImageHistogram(red, green, blue, luminance, 0)
            }

            val totalPixels = bitmap.width.toLong() * bitmap.height.toLong()
            val stride = if (totalPixels <= MAX_SAMPLES) 1 else
                ceil(sqrt(totalPixels.toDouble() / MAX_SAMPLES)).toInt().coerceAtLeast(1)
            var count = 0

            // Avoid allocating a full-image IntArray, which can be several
            // hundred megabytes for high-resolution photos.
            var y = 0
            while (y < bitmap.height) {
                var x = 0
                while (x < bitmap.width) {
                    val pixel = bitmap.getPixel(x, y)
                    if (Color.alpha(pixel) != 0) {
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        red[r]++
                        green[g]++
                        blue[b]++
                        val luma = (0.2126 * r + 0.7152 * g + 0.0722 * b)
                            .toInt().coerceIn(0, 255)
                        luminance[luma]++
                        count++
                    }
                    x += stride
                }
                y += stride
            }
            return ImageHistogram(red, green, blue, luminance, count)
        }
    }
}
