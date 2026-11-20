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
class RetouchProcessorsTest {
    @Test fun dodgeBurnPreservesAlphaAndChangesColor() {
        val b=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888);b.setPixel(0,0,Color.argb(90,80,80,80))
        val out=RetouchProcessors.dodgeBurn(b,1f,1f)
        assertEquals(90,Color.alpha(out.getPixel(0,0)))
        assertTrue(Color.red(out.getPixel(0,0))>80)
    }
    @Test fun healingChangesCenterWithoutModifyingSource() {
        val b=Bitmap.createBitmap(9,9,Bitmap.Config.ARGB_8888);b.eraseColor(Color.WHITE);b.setPixel(4,4,Color.BLACK)
        val healed=RetouchProcessors.spotHeal(b,4,4,2)
        assertTrue(Color.red(healed.getPixel(4,4))>0)
        assertEquals(Color.BLACK,b.getPixel(4,4))
    }
    @Test fun smudgeMixesTranslatedSampleAndPreservesAlpha() {
        val b=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888);b.setPixel(0,0,Color.RED);b.setPixel(1,0,Color.BLUE)
        val out=RetouchProcessors.smudge(b,1,0,.5f)
        assertEquals(255,Color.alpha(out.getPixel(0,0)))
        assertNotEquals(Color.RED,out.getPixel(0,0))
    }
    @Test fun canceledRetouchProcessorsAbortWithoutChangingSource() {
        val source=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);source.eraseColor(Color.GREEN)
        val signal=android.os.CancellationSignal().apply{cancel()}
        assertThrows(android.os.OperationCanceledException::class.java){RetouchProcessors.dodgeBurn(source,1f,.5f,cancellation=signal)}
        assertThrows(android.os.OperationCanceledException::class.java){RetouchProcessors.spotHeal(source,1,1,1,signal)}
        assertThrows(android.os.OperationCanceledException::class.java){RetouchProcessors.smudge(source,1,1,.5f,signal)}
        assertEquals(Color.GREEN,source.getPixel(0,0))
    }
}
