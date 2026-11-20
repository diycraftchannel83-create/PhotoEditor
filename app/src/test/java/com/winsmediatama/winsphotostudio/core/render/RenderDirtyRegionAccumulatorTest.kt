package com.winsmediatama.winsphotostudio.core.render

import org.junit.Assert.*
import org.junit.Test

class RenderDirtyRegionAccumulatorTest {
    @Test fun expandsAndClampsHaloToCanvas() {
        val canvas=RenderDirtyRect.canvas(100,80)
        val accumulator=RenderDirtyRegionAccumulator(canvas)
        accumulator.add(RenderDirtyRect(2,3,10,12),halo=4)
        assertEquals(listOf(RenderDirtyRect(0,0,14,16)),accumulator.snapshot())
    }
    @Test fun mergesNearbyRectangles() {
        val accumulator=RenderDirtyRegionAccumulator(RenderDirtyRect.canvas(100,100),fullCanvasAreaRatio=1f)
        accumulator.add(RenderDirtyRect(10,10,20,20))
        accumulator.add(RenderDirtyRect(21,12,30,25))
        assertEquals(1,accumulator.snapshot().size)
        assertEquals(RenderDirtyRect(10,10,30,25),accumulator.snapshot()[0])
    }
    @Test fun collapsesWhenDirtyCoverageThresholdReached() {
        val canvas=RenderDirtyRect.canvas(100,100)
        val accumulator=RenderDirtyRegionAccumulator(canvas,fullCanvasAreaRatio=.2f)
        accumulator.add(RenderDirtyRect(0,0,50,50))
        assertTrue(accumulator.isFullCanvas())
        assertEquals(listOf(canvas),accumulator.snapshot())
    }
    @Test fun collapsesWhenFragmentedBeyondLimit() {
        val canvas=RenderDirtyRect.canvas(100,100)
        val accumulator=RenderDirtyRegionAccumulator(canvas,maxRects=2,fullCanvasAreaRatio=1f,mergeMargin=0)
        accumulator.add(RenderDirtyRect(0,0,2,2))
        accumulator.add(RenderDirtyRect(10,10,12,12))
        accumulator.add(RenderDirtyRect(30,30,32,32))
        assertTrue(accumulator.isFullCanvas())
    }
    @Test fun clearResetsState() {
        val accumulator=RenderDirtyRegionAccumulator(RenderDirtyRect.canvas(50,50))
        accumulator.add(RenderDirtyRect(1,1,20,20))
        accumulator.clear()
        assertFalse(accumulator.isFullCanvas())
        assertTrue(accumulator.snapshot().isEmpty())
    }
}
