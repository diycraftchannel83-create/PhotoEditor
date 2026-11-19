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
class LevelsCurvesProcessorTest {
    @Test fun identityLevelsPreservePixelsAndAlpha() {
        val source=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);source.setPixel(0,0,Color.argb(100,120,40,220))
        val output=LevelsProcessor.apply(source,LevelsValues())
        assertEquals(source.getPixel(0,0),output.getPixel(0,0))
    }
    @Test fun invalidLevelsRangeIsRejected() {
        assertThrows(IllegalArgumentException::class.java){LevelsProcessor.apply(Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888),LevelsValues(composite=LevelsChannel(inputBlack=.8f,inputWhite=.2f)))}
    }
    @Test fun identityCurvesPreservePixel() {
        val source=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);source.setPixel(0,0,Color.argb(40,80,140,210))
        val output=CurvesProcessor.apply(source,CurvesValues())
        assertEquals(source.getPixel(0,0),output.getPixel(0,0))
    }
    @Test fun curvePointChangesMidtone() {
        val source=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);source.setPixel(0,0,Color.rgb(128,128,128))
        val values=CurvesValues(composite=listOf(CurvePoint(0f,0f),CurvePoint(.5f,.75f),CurvePoint(1f,1f)))
        assertTrue(Color.red(CurvesProcessor.apply(source,values).getPixel(0,0))>128)
    }
}
