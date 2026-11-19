package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class AlphaLockProcessorTest {
    @Test fun alphaLockRetainsOriginalLayerTransparency() {
        val old=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888);old.setPixel(0,0,Color.argb(0,0,0,0));old.setPixel(1,0,Color.argb(128,0,0,0))
        val edited=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888);edited.eraseColor(Color.RED)
        val result=AlphaLockProcessor.apply(old,edited)
        assertEquals(0,Color.alpha(result.getPixel(0,0)))
        assertEquals(128,Color.alpha(result.getPixel(1,0)))
        assertEquals(255,Color.red(result.getPixel(1,0)))
    }
}
