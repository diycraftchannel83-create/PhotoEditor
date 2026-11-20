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
class BasicAdjustmentProcessorTest {
    @Test fun defaultValuesPreserveImagePixels() {
        val source=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888)
        source.setPixel(0,0,Color.argb(90,20,100,200));source.setPixel(1,0,Color.WHITE)
        val output=BasicAdjustmentProcessor.apply(source,BasicAdjustmentValues())
        assertEquals(source.getPixel(0,0),output.getPixel(0,0))
        assertEquals(source.getPixel(1,0),output.getPixel(1,0))
    }
    @Test fun sourceRemainsUnmodifiedAndAlphaIsPreserved() {
        val source=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);source.setPixel(0,0,Color.argb(80,100,100,100))
        val sourcePixelBefore=source.getPixel(0,0)
        val sourceRedBefore=Color.red(sourcePixelBefore)
        val output=BasicAdjustmentProcessor.apply(source,BasicAdjustmentValues(exposure=1f))
        assertEquals(Color.alpha(sourcePixelBefore),Color.alpha(output.getPixel(0,0)))
        assertEquals(sourcePixelBefore,source.getPixel(0,0))
        assertEquals(sourceRedBefore,Color.red(source.getPixel(0,0)))
        assertTrue(Color.red(output.getPixel(0,0))>sourceRedBefore)
    }
    @Test fun canceledRenderStopsBeforeProcessingAndPreservesSource() {
        val source=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);source.setPixel(0,0,Color.RED)
        val cancellation=android.os.CancellationSignal().apply{cancel()}
        assertThrows(android.os.OperationCanceledException::class.java){BasicAdjustmentProcessor.apply(source,BasicAdjustmentValues(),cancellation)}
        assertEquals(Color.RED,source.getPixel(0,0))
    }
}
