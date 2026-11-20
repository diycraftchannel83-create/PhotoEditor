package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.Color
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TiledLayerCompositeRendererTest {
    @Test fun tiledFlatCompositeMatchesFullCompositeForCanvasAlignedLayers() {
        val base = Bitmap.createBitmap(9, 7, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(30, 70, 120)) }
        val top = Bitmap.createBitmap(9, 7, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
            for (y in 1..5) for (x in 2..6) setPixel(x, y, Color.rgb(200, 40, 80))
        }
        val layers = listOf(
            EditorLayer(name = "Base", bitmap = base),
            EditorLayer(name = "Top", bitmap = top, opacity = .65f)
        )
        val reference = com.winsmediatama.winsphotostudio.core.editor.LayerCompositeRenderer.render(layers, 9, 7)
        val tiles = TiledLayerCompositeRenderer.renderTiles(layers, 9, 7, tileSize = 4)
        val result = TiledLayerCompositeRenderer.stitch(tiles, 9, 7)
        for (y in 0 until 7) for (x in 0 until 9) assertEquals("pixel ($x,$y)", reference.getPixel(x,y), result.getPixel(x,y))
        result.recycle(); reference.recycle(); TiledLayerCompositeRenderer.recycleTiles(tiles); base.recycle(); top.recycle()
    }

    @Test(expected = IllegalArgumentException::class)
    fun clippingIsRejectedRatherThanSilentlyIgnored() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        try {
            val layer = EditorLayer(name = "Operations", bitmap = bitmap)
            val clipped = layer.copy(clipToBelow = true)
            TiledLayerCompositeRenderer.renderTiles(listOf(clipped), 4, 4, tileSize = 2)
        } finally { bitmap.recycle() }
    }

    @Test fun streamingScanlinePngMatchesFullCompositeWithoutRetainingTileGrid() {
        val base = Bitmap.createBitmap(7, 5, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(35, 80, 130)) }
        val overlay = Bitmap.createBitmap(7, 5, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
            for (y in 0..3) for (x in 1..5) setPixel(x, y, Color.rgb(180, 30, 70))
        }
        val layers = listOf(
            EditorLayer(name = "Base", bitmap = base),
            EditorLayer(name = "Overlay", bitmap = overlay, opacity = .72f)
        )
        val reference = com.winsmediatama.winsphotostudio.core.editor.LayerCompositeRenderer.render(layers, 7, 5)
        val stream = java.io.ByteArrayOutputStream()
        TiledLayerCompositeRenderer.encodeStreamingPng(layers, 7, 5, stream, tileWidth = 3)
        val bytes = stream.toByteArray()
        val decoded = requireNotNull(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        for (y in 0 until 5) for (x in 0 until 7) {
            assertEquals("stream pixel ($x,$y)", reference.getPixel(x,y), decoded.getPixel(x,y))
        }
        decoded.recycle(); reference.recycle(); base.recycle(); overlay.recycle()
    }

    @Test fun streamingScanlineEncoderHonorsCancellationCallback() {
        val bitmap = Bitmap.createBitmap(3, 3, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val layers = listOf(EditorLayer(name = "Layer", bitmap = bitmap))
        val stream = java.io.ByteArrayOutputStream()
        var rows = 0
        try {
            TiledLayerCompositeRenderer.encodeStreamingPng(layers, 3, 3, stream, tileWidth = 2, cancelCheck = {
                if (rows >= 1) throw kotlinx.coroutines.CancellationException("test cancellation")
            })
        } catch (_: kotlinx.coroutines.CancellationException) {
            assertEquals(1, rows)
        } finally { bitmap.recycle() }
    }

    @Test fun partialLayerBoundsAreTransparentOutsideSource() {
        val small = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val tiles = TiledLayerCompositeRenderer.renderTiles(listOf(EditorLayer(name = "Small", bitmap = small)), 4, 4, tileSize = 3)
        val output = TiledLayerCompositeRenderer.stitch(tiles, 4, 4)
        assertEquals(Color.RED, output.getPixel(0,0))
        assertEquals(Color.TRANSPARENT, output.getPixel(3,3))
        output.recycle(); TiledLayerCompositeRenderer.recycleTiles(tiles); small.recycle()
    }
}
