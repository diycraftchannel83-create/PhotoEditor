package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.os.CancellationSignal
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues

/**
 * Tile-backed Basic Adjust renderer. Outputs are retained as independently owned core tiles,
 * allowing downstream consumers to avoid allocating one full-resolution output bitmap.
 * Sharpen uses a 1-pixel halo because its neighborhood reference is radius 1.
 */
object TiledBasicAdjustmentProcessor {
    fun applyTiles(
        source: Bitmap,
        values: BasicAdjustmentValues,
        tileSize: Int = 512,
        cancellation: CancellationSignal? = null
    ): List<BitmapHaloTileProcessor.HaloTile> {
        val halo = if (values.sharpen > 0f) 1 else 0
        return BitmapHaloTileProcessor.process(
            source = source,
            tileSize = tileSize,
            halo = halo,
            cancellation = cancellation
        ) { expandedTile, _, signal ->
            BasicAdjustmentProcessor.apply(expandedTile, values, signal)
        }
    }

    /** Optional bounded flattening for preview/consumer paths that still require one bitmap. */
    fun flatten(
        tiles: List<BitmapHaloTileProcessor.HaloTile>,
        width: Int,
        height: Int,
        maxPixels: Long = 24_000_000L,
        cancellation: CancellationSignal? = null
    ): Bitmap = BitmapHaloTileProcessor.stitch(tiles, width, height, maxPixels, cancellation)
}
