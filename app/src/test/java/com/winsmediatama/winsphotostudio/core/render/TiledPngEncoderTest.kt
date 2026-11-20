package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TiledPngEncoderTest {
    @Test fun streamingEncoderProducesDecodablePngMatchingTiles() {
        val source = Bitmap.createBitmap(5, 4, Bitmap.Config.ARGB_8888)
        for (y in 0 until 4) for (x in 0 until 5) {
            source.setPixel(x, y, Color.argb((x+y)*20, x*40, y*50, (x+y)*25))
        }
        val tiles = BitmapTileProcessor.process(source, tileSize = 2) { input, _, _ ->
            input.copy(Bitmap.Config.ARGB_8888, false)
        }
        val stream = ByteArrayOutputStream()
        TiledPngEncoder.encode(tiles, 5, 4, stream)
        val encoded = stream.toByteArray()
        assertTrue(encoded.size > 32)
        val decoded = BitmapFactory.decodeByteArray(encoded, 0, encoded.size)
        assertEquals(5, decoded.width); assertEquals(4, decoded.height)
        for (y in 0 until 4) for (x in 0 until 5) assertEquals(source.getPixel(x,y), decoded.getPixel(x,y))
        decoded.recycle(); BitmapTileProcessor.recycleTiles(tiles); source.recycle()
    }

    @Test(expected = IllegalArgumentException::class)
    fun encoderRejectsMissingTileCoverage() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try {
            TiledPngEncoder.encode(listOf(BitmapTileProcessor.ProcessedTile(Rect(0,0,2,2),bitmap)), 3, 2, ByteArrayOutputStream())
        } finally { bitmap.recycle() }
    }
}
