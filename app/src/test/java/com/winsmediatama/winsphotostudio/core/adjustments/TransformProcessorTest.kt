package com.winsmediatama.winsphotudio.core.adjustments

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TransformProcessorTest {
    @Test
    fun cropAndResizeRespectRequestedOutputDimensions() {
        val source=Bitmap.createBitmap(8,6,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val result=TransformProcessor.apply(source,.25f,.25f,.75f,.75f,0f,false,false,3,2)
        assertEquals(3,result.width);assertEquals(2,result.height)
        assertEquals(Color.RED,result.getPixel(0,0))
        result.recycle();source.recycle()
    }

    @Test
    fun quarterTurnRotationSwapsBoundingDimensions() {
        val source=Bitmap.createBitmap(4,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.GREEN)}
        val result=TransformProcessor.apply(source,0f,0f,1f,1f,90f,false,false)
        assertEquals(2,result.width);assertEquals(4,result.height)
        result.recycle();source.recycle()
    }

    @Test
    fun horizontalFlipReversesPixels() {
        val source=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.RED);setPixel(1,0,Color.BLUE)}
        val result=TransformProcessor.apply(source,0f,0f,1f,1f,0f,true,false)
        assertEquals(Color.BLUE,result.getPixel(0,0));assertEquals(Color.RED,result.getPixel(1,0))
        result.recycle();source.recycle()
    }
}
