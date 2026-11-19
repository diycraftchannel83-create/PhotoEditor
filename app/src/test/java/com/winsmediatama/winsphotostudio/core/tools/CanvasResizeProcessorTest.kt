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
class CanvasResizeProcessorTest {
    @Test fun canvasExpansionCentersImageByDefaultAnchor() {
        val src=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);src.eraseColor(Color.RED)
        val out=CanvasResizeProcessor.resize(src,4,4)
        assertEquals(Color.RED,out.getPixel(1,1));assertEquals(Color.TRANSPARENT,out.getPixel(0,0))
    }
    @Test fun topLeftAnchorKeepsOrigin() {
        val src=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);src.eraseColor(Color.BLUE)
        val out=CanvasResizeProcessor.resize(src,4,4,CanvasAnchor.TOP_LEFT)
        assertEquals(Color.BLUE,out.getPixel(0,0));assertEquals(Color.TRANSPARENT,out.getPixel(3,3))
    }
    @Test fun invalidDimensionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java){CanvasResizeProcessor.resize(Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888),0,2)}
    }
}
