package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class TransformProcessorTest {
    @Test fun cropResizeAndFlipKeepsDimensions() {
        val source=Bitmap.createBitmap(4,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED);setPixel(0,0,Color.BLUE)}
        val result=TransformProcessor.apply(source,TransformSpec(left=0f,top=0f,right=.5f,bottom=1f,flipHorizontal=true,outputWidth=3,outputHeight=4))
        assertEquals(3,result.width);assertEquals(4,result.height)
        assertEquals(Color.BLUE,source.getPixel(0,0))
    }
    @Test fun rotationProducesExpectedBounds() {
        val source=Bitmap.createBitmap(3,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.GREEN)}
        val result=TransformProcessor.apply(source,TransformSpec(rotationDegrees=90f))
        assertEquals(2,result.width);assertEquals(3,result.height)
    }
    @Test fun rejectsInvalidTransformBounds() {
        val source=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888)
        assertThrows(IllegalArgumentException::class.java){TransformProcessor.apply(source,TransformSpec(left=.8f,right=.2f))}
    }
}
