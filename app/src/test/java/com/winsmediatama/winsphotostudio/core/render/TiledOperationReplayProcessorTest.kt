package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack

@RunWith(RobolectricTestRunner::class)
class TiledOperationReplayProcessorTest {
    @Test fun tiledBasicAdjustmentReplayMatchesFullStack() {
        val source=Bitmap.createBitmap(13,9,Bitmap.Config.ARGB_8888)
        for(y in 0 until source.height) for(x in 0 until source.width)
            source.setPixel(x,y,Color.argb(255,(x*17)%256,(y*29)%256,(x+y)*11%256)
        val operation=EditOperation.BasicAdjust(values=BasicAdjustmentValues(brightness=.12f,contrast=.08f,saturation=.2f))
        val reference=NonDestructiveEditStack(source,listOf(operation)).render()
        val tiles=TiledOperationReplayProcessor.renderTiles(source,listOf(operation),tileSize=5)
        val assembled=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
        try {
            tiles.forEach { tile ->
                val canvas=android.graphics.Canvas(assembled)
                canvas.drawBitmap(tile.bitmap,tile.rect.left.toFloat(),tile.rect.top.toFloat(),null)
            }
            for(y in 0 until source.height) for(x in 0 until source.width)
                assertEquals("pixel $x,$y",reference.getPixel(x,y),assembled.getPixel(x,y))
        } finally {
            TiledOperationReplayProcessor.recycleTiles(tiles)
            source.recycle();reference.recycle();assembled.recycle()
        }
    }

    @Test fun unsupportedSpatialOperationsAreRejectedBeforeProcessing() {
        val source=Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888)
        val crop=EditOperation.Crop(left=0f,top=0f,right=.5f,bottom=.5f)
        assertFalse(TiledOperationReplayProcessor.supports(listOf(crop)))
        source.recycle()
    }
}
