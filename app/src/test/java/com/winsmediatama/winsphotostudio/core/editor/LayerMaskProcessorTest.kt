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
class LayerMaskProcessorTest {
    @Test fun whiteMaskKeepsLayerAlphaAndBlackMaskHides() {
        val source=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888)
        source.setPixel(0,0,Color.RED);source.setPixel(1,0,Color.RED)
        val mask=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888)
        mask.setPixel(0,0,Color.WHITE);mask.setPixel(1,0,Color.BLACK)
        val result=LayerMaskProcessor.apply(source,mask)
        assertEquals(255,Color.alpha(result.getPixel(0,0)))
        assertEquals(0,Color.alpha(result.getPixel(1,0)))
    }
    @Test fun combineMultiplyIntersectsRevealMasks() {\n        val a=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.rgb(128,128,128))}\n        val b=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.rgb(128,128,128))}\n        val combined=LayerMaskProcessor.combineMultiply(a,b)\n        assertEquals(64,Color.red(combined.getPixel(0,0)))\n    }\n    @Test fun inversionSwapsWhiteAndBlack() {
        val mask=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);mask.setPixel(0,0,Color.WHITE)
        assertEquals(Color.BLACK,LayerMaskProcessor.invert(mask).getPixel(0,0))
    }
}
