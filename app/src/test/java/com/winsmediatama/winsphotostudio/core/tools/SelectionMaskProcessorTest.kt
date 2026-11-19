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
    @Test fun featherProducesIntermediateValues() {
        val m=SelectionMaskProcessor.rectangle(8,8,.25f,.25f,.75f,.75f)
        val feathered=SelectionMaskProcessor.feather(m,1)
        assertTrue(Color.red(feathered.getPixel(1,3)) in 1..254)
    }
}
