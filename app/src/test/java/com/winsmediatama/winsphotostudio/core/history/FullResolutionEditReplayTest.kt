package com.winsmediatama.winsphotostudio.core.history

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class FullResolutionEditReplayTest {
    @Test
    fun dimensionBoundOperationsScaleAndNormalizedOperationsStaySame() {
        val ops = listOf(
            EditOperation.Transform(left=.1f,top=.1f,right=.9f,bottom=.9f,outputWidth=100,outputHeight=80),
            EditOperation.Perspective(outputWidth=120,outputHeight=90),
            EditOperation.CanvasSize(width=200,height=160),
            EditOperation.Crop(left=.2f,top=.2f,right=.8f,bottom=.8f)
        )
        val scaled = FullResolutionEditReplay.scaleOperations(
            ops,
            FullResolutionEditReplay.ReplayDimensions(100,80,400,320)
        )
        assertEquals(400, (scaled[0] as EditOperation.Transform).outputWidth)
        assertEquals(320, (scaled[0] as EditOperation.Transform).outputHeight)
        assertEquals(480, (scaled[1] as EditOperation.Perspective).outputWidth)
        assertEquals(360, (scaled[1] as EditOperation.Perspective).outputHeight)
        assertEquals(800, (scaled[2] as EditOperation.CanvasSize).width)
        assertEquals(640, (scaled[2] as EditOperation.CanvasSize).height)
        assertEquals(.2f, (scaled[3] as EditOperation.Crop).left)
        assertEquals(.8f, (scaled[3] as EditOperation.Crop).right)
    }

    @Test
    fun zeroOutputDimensionsRemainAutomaticAndInvalidScaleIsRejected() {
        val transform=EditOperation.Transform(outputWidth=0,outputHeight=0)
        val scaled=FullResolutionEditReplay.scaleOperations(
            listOf(transform),
            FullResolutionEditReplay.ReplayDimensions(100,100,300,200)
        ).single() as EditOperation.Transform
        assertEquals(0,scaled.outputWidth)
        assertEquals(0,scaled.outputHeight)
        assertTrue(runCatching {
            FullResolutionEditReplay.ReplayDimensions(0,100,200,200)
        }.isFailure)
    }

    @Test
    fun rejectsSourceAndOperationDimensionsOverPixelBudget() {
        assertTrue(runCatching {
            FullResolutionEditReplay.ReplayDimensions(100,100,5000,5000)
        }.isFailure)
        val dimensions=FullResolutionEditReplay.ReplayDimensions(100,100,400,400)
        val oversized=EditOperation.CanvasSize(width=10000,height=10000)
        assertTrue(runCatching { FullResolutionEditReplay.scaleOperations(listOf(oversized),dimensions) }.isFailure)
    }

    @Test
    fun replayReturnsNewBitmapAndPreservesSource() {
        val source=Bitmap.createBitmap(8,6,Bitmap.Config.ARGB_8888)
        source.eraseColor(android.graphics.Color.GREEN)
        val rendered=FullResolutionEditReplay.render(source, emptyList(),4,3)
        assertEquals(8,rendered.width)
        assertEquals(6,rendered.height)
        assertEquals(android.graphics.Color.GREEN,source.getPixel(0,0))
        assertTrue(rendered !== source)
        rendered.recycle()
        source.recycle()
    }
}
