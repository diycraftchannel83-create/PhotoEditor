package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class SelectionMaskProcessorTest {
    @Test fun magicWandSelectsContiguousColorRegionOnly() {
        val source=android.graphics.Bitmap.createBitmap(3,1,android.graphics.Bitmap.Config.ARGB_8888)
        source.setPixel(0,0,Color.RED);source.setPixel(1,0,Color.RED);source.setPixel(2,0,Color.BLUE)
        val mask=SelectionMaskProcessor.magicWand(source,0,0,.05f)
        assertEquals(255,Color.red(mask.getPixel(0,0)))
        assertEquals(255,Color.red(mask.getPixel(1,0)))
        assertEquals(0,Color.red(mask.getPixel(2,0)))
    }
    @Test fun polygonSelectionIncludesInteriorAndExcludesOutside() {
        val mask=SelectionMaskProcessor.polygon(10,10,listOf(.2f to .2f,.8f to .2f,.8f to .8f,.2f to .8f))
        assertEquals(255,Color.red(mask.getPixel(5,5)))
        assertEquals(0,Color.red(mask.getPixel(0,0)))
    }
    @Test fun colorRangeSelectsMatchingColorAndRejectsDistantColor() {
        val source=android.graphics.Bitmap.createBitmap(2,1,android.graphics.Bitmap.Config.ARGB_8888)
        source.setPixel(0,0,Color.RED);source.setPixel(1,0,Color.BLUE)
        val mask=SelectionMaskProcessor.colorRange(source,Color.RED,.1f)
        assertEquals(255,Color.red(mask.getPixel(0,0)))
        assertEquals(0,Color.red(mask.getPixel(1,0)))
    }
    @Test fun colorRangeMaskPreservesSourceAlphaAndMagicWandIgnoresTransparentPixels() {
        val source=android.graphics.Bitmap.createBitmap(2,1,android.graphics.Bitmap.Config.ARGB_8888)
        source.setPixel(0,0,Color.argb(255,255,0,0));source.setPixel(1,0,Color.argb(0,255,0,0))
        val range=SelectionMaskProcessor.colorRange(source,Color.RED,.1f)
        assertEquals(255,Color.alpha(range.getPixel(0,0)))
        assertEquals(0,Color.alpha(range.getPixel(1,0)))
        val wand=SelectionMaskProcessor.magicWand(source,0,0,.1f)
        assertEquals(255,Color.red(wand.getPixel(0,0)))
        assertEquals(0,Color.red(wand.getPixel(1,0)))
    }
    @Test fun rectangleCreatesExpectedSelection() {
        val m=SelectionMaskProcessor.rectangle(10,10,.2f,.2f,.6f,.6f)
        assertEquals(255,Color.red(m.getPixel(3,3)))
        assertEquals(0,Color.red(m.getPixel(0,0)))
    }
    @Test fun ellipseSelectsCenterButNotCorner() {
        val m=SelectionMaskProcessor.ellipse(10,10,0f,0f,1f,1f)
        assertEquals(255,Color.red(m.getPixel(5,5)))
        assertEquals(0,Color.red(m.getPixel(0,0)))
    }
    @Test fun invertSwapsSelectionValues() {
        val m=SelectionMaskProcessor.rectangle(2,1,0f,0f,.5f,1f)
        val inverted=SelectionMaskProcessor.invert(m)
        assertEquals(0,Color.red(inverted.getPixel(0,0)))
        assertEquals(255,Color.red(inverted.getPixel(1,0)))
    }
    @Test fun expansionAddsSelectedPixels() {
        val m=SelectionMaskProcessor.rectangle(5,5,.4f,.4f,.6f,.6f)
        val expanded=SelectionMaskProcessor.expandContract(m,1)
        assertTrue(Color.red(expanded.getPixel(1,1))>0)
    }
    @Test fun morphologyAndFeatherRemainBoundedAtImageEdges() {
        val mask=SelectionMaskProcessor.rectangle(3,3,0f,0f,.34f,.34f)
        val expanded=SelectionMaskProcessor.expandContract(mask,1)
        assertEquals(255,Color.red(expanded.getPixel(0,0)))
        assertEquals(0,Color.red(expanded.getPixel(2,2)))
        val feather=SelectionMaskProcessor.feather(mask,1)
        assertTrue(Color.red(feather.getPixel(0,0)) in 1..255)
        assertEquals(0,Color.red(feather.getPixel(2,2)))
    }
    @Test fun featherProducesIntermediateValues() {
        val m=SelectionMaskProcessor.rectangle(8,8,.25f,.25f,.75f,.75f)
        val feathered=SelectionMaskProcessor.feather(m,1)
        assertTrue(Color.red(feathered.getPixel(1,3)) in 1..254)
    }
    @Test fun canceledMaskOperationsAbortAndLeaveSourceMaskUntouched() {
        val source=android.graphics.Bitmap.createBitmap(2,2,android.graphics.Bitmap.Config.ARGB_8888);source.eraseColor(Color.RED)
        val mask=SelectionMaskProcessor.rectangle(2,2,0f,0f,.5f,.5f)
        val signal=android.os.CancellationSignal().apply{cancel()}
        assertThrows(android.os.OperationCanceledException::class.java){SelectionMaskProcessor.magicWand(source,0,0,.1f,signal)}
        assertThrows(android.os.OperationCanceledException::class.java){SelectionMaskProcessor.colorRange(source,Color.RED,.2f,0f,signal)}
        assertThrows(android.os.OperationCanceledException::class.java){SelectionMaskProcessor.polygon(2,2,listOf(0f to 0f,1f to 0f,1f to 1f),signal)}
        assertThrows(android.os.OperationCanceledException::class.java){SelectionMaskProcessor.invert(mask,signal)}
        assertEquals(255,Color.red(mask.getPixel(0,0)))
        assertEquals(0,Color.red(mask.getPixel(1,1)))
    }
}
