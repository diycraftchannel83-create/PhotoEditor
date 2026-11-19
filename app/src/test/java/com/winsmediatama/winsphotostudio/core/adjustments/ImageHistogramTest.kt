package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageHistogramTest {
    @Test
    fun largeImageSamplingIsBoundedAndPreservesFlatColorDistribution() {
        val bitmap = Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(25, 80, 160))

        val histogram = ImageHistogram.from(bitmap)

        assertEquals(true, histogram.sampleCount <= ImageHistogram.MAX_SAMPLES)
        assertEquals(histogram.sampleCount, histogram.red[25])
        assertEquals(histogram.sampleCount, histogram.green[80])
        assertEquals(histogram.sampleCount, histogram.blue[160])
        bitmap.recycle()
    }

    @Test
    fun countsRgbAndLuminanceBinsAndIgnoresTransparentPixels() {
        val bitmap = Bitmap.createBitmap(3, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.RED)
        bitmap.setPixel(1, 0, Color.GREEN)
        bitmap.setPixel(2, 0, Color.TRANSPARENT)

        val histogram = ImageHistogram.from(bitmap)

        assertEquals(2, histogram.sampleCount)
        assertEquals(1, histogram.red[255])
        assertEquals(1, histogram.red[0])
        assertEquals(1, histogram.green[255])
        assertEquals(1, histogram.green[0])
        assertEquals(2, histogram.blue[0])
        assertEquals(1, histogram.luminance[54]) // red luminance\n        assertEquals(1, histogram.luminance[182]) // green luminance
    }
}
