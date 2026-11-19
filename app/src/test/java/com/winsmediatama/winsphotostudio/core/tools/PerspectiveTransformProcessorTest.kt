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
class PerspectiveTransformProcessorTest {
    @Test fun identityQuadPreservesDimensionsAndPixel() {
        val source=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);source.eraseColor(Color.RED)
        val result=PerspectiveTransformProcessor.warp(source,PerspectiveQuad.IDENTITY)
        assertEquals(2,result.width);assertEquals(2,result.height);assertEquals(Color.RED,result.getPixel(0,0))
    }
    @Test fun rejectsInvalidQuad() {
        val source=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888)
        val invalid=PerspectiveQuad(0f to 0f,1f to 1f,0f to 1f,1f to 0f)
        assertThrows(IllegalArgumentException::class.java){PerspectiveTransformProcessor.warp(source,invalid)}
    }
}
