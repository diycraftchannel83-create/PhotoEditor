package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.Color
import android.os.CancellationSignal
import android.os.OperationCanceledException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapTileProcessorTest {
    @Test fun processingProducesNonOverlappingTilesAndPreservesSource() {
        val source=Bitmap.createBitmap(520,300,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val tiles=BitmapTileProcessor.process(source,tileSize=256){tile,_,_->tile.copy(Bitmap.Config.ARGB_8888,false)}
        assertEquals(6,tiles.size)
        assertEquals(520,source.width)
        assertEquals(Color.RED,source.getPixel(0,0))
        assertEquals(512,tiles.last().sourceRect.left)
        assertEquals(8,tiles.last().sourceRect.width())
        BitmapTileProcessor.recycleTiles(tiles)
        source.recycle()
    }

    @Test fun stitchRespectsPixelLimitAndMapsTileCoordinates() {
        val source=Bitmap.createBitmap(4,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLUE)}
        val tiles=BitmapTileProcessor.process(source,2){tile,_,_->tile.copy(Bitmap.Config.ARGB_8888,false)}
        val stitched=BitmapTileProcessor.stitch(tiles,4,2)
        assertEquals(Color.BLUE,stitched.getPixel(3,1))
        stitched.recycle()
        BitmapTileProcessor.recycleTiles(tiles)
        source.recycle()
    }

    @Test(expected=IllegalArgumentException::class)
    fun stitchRejectsOversizeCanvas() {
        BitmapTileProcessor.stitch(emptyList(),100,100,maxPixels=999)
    }

    @Test fun preCancelledProcessingThrowsAndDoesNotLeakOutputTiles() {
        val source=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888)
        val signal=CancellationSignal().apply{cancel()}
        var called=false
        try {
            BitmapTileProcessor.process(source,4,signal){tile,_,_->called=true;tile.copy(Bitmap.Config.ARGB_8888,false)}
        } catch(_:OperationCanceledException) {
            assertFalse(called)
        }
        source.recycle()
    }
}
