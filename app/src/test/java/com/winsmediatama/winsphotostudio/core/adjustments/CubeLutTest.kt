package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class CubeLutTest {
    @Test fun oneDimensionalIdentityPreservesColor() {
        val lut=CubeLut.parse("LUT_1D_SIZE 2\n0 0 0\n1 1 1")
        val bitmap=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);bitmap.setPixel(0,0,Color.argb(127,128,64,200))
        val result=lut.apply(bitmap)
        assertEquals(127,Color.alpha(result.getPixel(0,0)))
        assertTrue(kotlin.math.abs(128-Color.red(result.getPixel(0,0)))<=1)
        assertTrue(kotlin.math.abs(64-Color.green(result.getPixel(0,0)))<=1)
        assertTrue(kotlin.math.abs(200-Color.blue(result.getPixel(0,0)))<=1)
    }
    @Test fun rejectsTableLengthMismatch() {
        assertThrows(IllegalArgumentException::class.java){CubeLut.parse("LUT_1D_SIZE 3\n0 0 0\n1 1 1")}
    }
}
