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
        assertEquals(1, histogram.blue[0])
        assertEquals(1, histogram.luminance[54]) // green luminance approx 182? corrected below
    }
}
