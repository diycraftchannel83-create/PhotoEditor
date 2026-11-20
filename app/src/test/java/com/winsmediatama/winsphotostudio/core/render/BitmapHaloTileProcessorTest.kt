package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.CancellationSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapHaloTileProcessorTest {
    @Test fun expandedRectsClampToSourceAndCoreTilesStayNonOverlapping() {
        val source = Bitmap.createBitmap(520, 300, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val tiles = BitmapHaloTileProcessor.process(source, tileSize = 256, halo = 12) { input, _, _ ->
            input.copy(Bitmap.Config.ARGB_8888, false)
        }
        assertEquals(6, tiles.size)
        assertEquals(Rect(0, 0, 256, 256), tiles.first().coreRect)
        assertEquals(Rect(0, 0, 268, 268), tiles.first().expandedRect)
        assertEquals(Rect(500, 244, 520, 300), tiles.last().expandedRect)
        tiles.forEach { assertEquals(it.coreRect.width(), it.bitmap.width); assertEquals(it.coreRect.height(), it.bitmap.height) }
        BitmapHaloTileProcessor.recycleTiles(tiles)
        source.recycle()
    }

    @Test fun haloProcessingMatchesWholeImageForRadiusOneNeighborhoodAtTileSeams() {
        val source = Bitmap.createBitmap(7, 5, Bitmap.Config.ARGB_8888)
        for (y in 0 until source.height) for (x in 0 until source.width) {
            source.setPixel(x, y, Color.rgb(x * 30, y * 40, (x + y) * 15))
        }
        fun horizontalAverage(bitmap: Bitmap): Bitmap {
            val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                val left = bitmap.getPixel((x - 1).coerceAtLeast(0), y)
                val center = bitmap.getPixel(x, y)
                val right = bitmap.getPixel((x + 1).coerceAtMost(bitmap.width - 1), y)
                val r = (Color.red(left) + Color.red(center) + Color.red(right)) / 3
                val g = (Color.green(left) + Color.green(center) + Color.green(right)) / 3
                val b = (Color.blue(left) + Color.blue(center) + Color.blue(right)) / 3
                out.setPixel(x, y, Color.rgb(r, g, b))
            }
            return out
        }
        val reference = horizontalAverage(source)
        val tiles = BitmapHaloTileProcessor.process(source, tileSize = 3, halo = 1) { expanded, _, _ ->
            horizontalAverage(expanded)
        }
        val stitched = BitmapHaloTileProcessor.stitch(tiles, source.width, source.height)
        for (y in 0 until source.height) for (x in 0 until source.width) {
            assertEquals("pixel ($x,$y)", reference.getPixel(x,y), stitched.getPixel(x,y))
        }
        stitched.recycle()
        reference.recycle()
        BitmapHaloTileProcessor.recycleTiles(tiles)
        source.recycle()
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeHaloRejected() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        try { BitmapHaloTileProcessor.process(source, halo = -1) { tile, _, _ -> tile } }
        finally { source.recycle() }
    }

    @Test fun cancellationBeforeFirstTileStopsProcessing() {
        val source = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val signal = CancellationSignal().apply { cancel() }
        var invoked = false
        try {
            BitmapHaloTileProcessor.process(source, tileSize = 4, halo = 1, cancellation = signal) { input, _, _ ->
                invoked = true
                input
            }
        } catch (_: android.os.OperationCanceledException) {
            assertTrue(!invoked)
        } finally {
            source.recycle()
        }
    }
}
