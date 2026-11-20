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
class EffectProcessorTest {
    @Test fun zeroStrengthVignettePreservesPixels() {
        val b=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);b.eraseColor(Color.RED)
        assertEquals(Color.RED,EffectProcessor.vignette(b,0f).getPixel(0,0))
    }
    @Test fun blurSoftensHardEdge() {
        val b=Bitmap.createBitmap(3,1,Bitmap.Config.ARGB_8888);b.setPixel(0,0,Color.BLACK);b.setPixel(1,0,Color.WHITE);b.setPixel(2,0,Color.BLACK)
        val result=EffectProcessor.gaussianBlur(b,1)
        assertTrue(Color.red(result.getPixel(1,0))<255)
    }
    @Test fun grainIsDeterministicForSeed() {
        val b=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);b.eraseColor(Color.GRAY)
        assertEquals(EffectProcessor.grain(b,.5f,42).getPixel(0,0),EffectProcessor.grain(b,.5f,42).getPixel(0,0))
    }
    @Test fun cancelledEffectRenderStopsAndKeepsSourceBitmap() {
        val source=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);source.eraseColor(Color.CYAN)
        val signal=android.os.CancellationSignal().apply{cancel()}
        assertThrows(android.os.OperationCanceledException::class.java){EffectProcessor.gaussianBlur(source,4,signal)}
        assertThrows(android.os.OperationCanceledException::class.java){EffectProcessor.motionBlur(source,4,45f,signal)}
        assertThrows(android.os.OperationCanceledException::class.java){EffectProcessor.vignette(source,.8f,signal)}
        assertThrows(android.os.OperationCanceledException::class.java){EffectProcessor.grain(source,.4f,12,signal)}
        assertEquals(Color.CYAN,source.getPixel(0,0))
    }
}
