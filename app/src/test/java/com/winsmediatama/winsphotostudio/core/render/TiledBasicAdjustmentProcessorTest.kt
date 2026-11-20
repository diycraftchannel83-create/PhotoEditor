package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.Color
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TiledBasicAdjustmentProcessorTest {
    @Test fun tiledBasicAdjustMatchesWholeImageIncludingSharpenTileEdges() {
        val source = Bitmap.createBitmap(11, 7, Bitmap.Config.ARGB_8888)
        for (y in 0 until source.height) for (x in 0 until source.width) {
            source.setPixel(x, y, Color.rgb((x * 21).coerceAtMost(255), (y * 35).coerceAtMost(255), ((x + y) * 12).coerceAtMost(255)))
        }
        val values = BasicAdjustmentValues(brightness = .08f, contrast = .12f, saturation = .2f, sharpen = .65f)
        val reference = BasicAdjustmentProcessor.apply(source, values)
        val tiles = TiledBasicAdjustmentProcessor.applyTiles(source, values, tileSize = 4)
        val stitched = TiledBasicAdjustmentProcessor.flatten(tiles, source.width, source.height)
        for (y in 0 until source.height) for (x in 0 until source.width) {
            assertEquals("mismatch at ($x,$y)", reference.getPixel(x, y), stitched.getPixel(x, y))
        }
        stitched.recycle()
        reference.recycle()
        BitmapHaloTileProcessor.recycleTiles(tiles)
        source.recycle()
    }
}
