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
    @Test fun nonIdentityQuadBuildsPerspectiveMatrixAndRenders() {
        val source=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val quad=PerspectiveQuad(.1f to .1f,.9f to .05f,.95f to .9f,.05f to .95f)
        val result=PerspectiveTransformProcessor.warp(source,quad,16,16)
        assertEquals(16,result.width);assertEquals(16,result.height)
        assertEquals(Color.RED,result.getPixel(8,8))
        result.recycle();source.recycle()
    }
    @Test fun rejectsInvalidQuad() {
        val source=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888)
        val invalid=PerspectiveQuad(0f to 0f,1f to 1f,0f to 1f,1f to 0f)
        assertThrows(IllegalArgumentException::class.java){PerspectiveTransformProcessor.warp(source,invalid)}
    }
    @Test fun cancellationAndOversizedOutputAreRejectedBeforeWarp() {
        val source=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);source.setPixel(0,0,Color.BLUE)
        val signal=android.os.CancellationSignal().apply{cancel()}
        assertThrows(android.os.OperationCanceledException::class.java){PerspectiveTransformProcessor.warp(source,PerspectiveQuad.IDENTITY,cancellation=signal)}
        assertThrows(IllegalArgumentException::class.java){PerspectiveTransformProcessor.warp(source,PerspectiveQuad.IDENTITY,5000,5000)}
        assertEquals(Color.BLUE,source.getPixel(0,0))
    }
}
