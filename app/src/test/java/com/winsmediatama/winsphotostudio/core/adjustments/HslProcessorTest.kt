package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HslProcessorTest {
    @Test
    fun identityAdjustmentsPreservePixelAndAlpha() {
        val source = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        source.setPixel(0, 0, AndroidColor.argb(128, 255, 0, 0))

        val result = HslProcessor.apply(source, HslAdjustments())

        assertEquals(128, AndroidColor.alpha(result.getPixel(0, 0)))
        assertEquals(255, AndroidColor.red(result.getPixel(0, 0)))
        assertEquals(0, AndroidColor.green(result.getPixel(0, 0)))
        assertEquals(0, AndroidColor.blue(result.getPixel(0, 0)))
    }

    @Test
    fun saturationAdjustmentChangesSelectedHueRange() {
        val source = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        source.setPixel(0, 0, AndroidColor.RED)

        val result = HslProcessor.apply(
            source,
            HslAdjustments(
                ranges = mapOf(
                    HslColorRange.RED to HslRangeAdjustment(saturation = -0.5f)
                )
            )
        )

        val adjusted = result.getPixel(0, 0)
        assertNotEquals(AndroidColor.RED, adjusted)
        assertTrue(AndroidColor.red(adjusted) > AndroidColor.green(adjusted))
        assertEquals(AndroidColor.RED, source.getPixel(0, 0))
    }
}
