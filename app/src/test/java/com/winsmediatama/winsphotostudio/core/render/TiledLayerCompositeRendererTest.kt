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

    @Test fun streamedDifferenceBlendMatchesFullComposite() {
        val base=Bitmap.createBitmap(17,9,Bitmap.Config.ARGB_8888).apply{
            for(y in 0 until height)for(x in 0 until width)setPixel(x,y,Color.rgb((x*11)%255,(y*23)%255,120))
        }
        val overlay=Bitmap.createBitmap(17,9,Bitmap.Config.ARGB_8888).apply{
            for(y in 0 until height)for(x in 0 until width)setPixel(x,y,Color.argb(if(x<9)220 else 100,180,40,(x*9)%255))
        }
        val layers=listOf(
            EditorLayer(name="Base",bitmap=base),
            EditorLayer(name="Difference",bitmap=overlay,opacity=.63f,blendMode=com.winsmediatama.winsphotostudio.core.editor.EditorBlendMode.DIFFERENCE)
        )
        val reference=com.winsmediatama.winsphotostudio.core.editor.LayerCompositeRenderer.render(layers,17,9)
        val stream=java.io.ByteArrayOutputStream()
        try{
            TiledLayerCompositeRenderer.encodeStreamingPng(layers,17,9,stream,tileWidth=5)
            val decoded=requireNotNull(android.graphics.BitmapFactory.decodeByteArray(stream.toByteArray(),0,stream.size()))
            for(y in 0 until 9)for(x in 0 until 17)
                assertEquals("difference pixel ($x,$y)",reference.getPixel(x,y),decoded.getPixel(x,y))
            decoded.recycle()
        }finally{reference.recycle();base.recycle();overlay.recycle()}
    }

    @Test fun streamedClippingAlphaMatchesFullComposite() {
        val base=Bitmap.createBitmap(39,33,Bitmap.Config.ARGB_8888).apply{
            eraseColor(Color.TRANSPARENT)
            for(y in 5 until 28)for(x in 3 until 35)setPixel(x,y,Color.argb(160,40,90,160))
        }
        val clipped=Bitmap.createBitmap(39,33,Bitmap.Config.ARGB_8888).apply{
            eraseColor(Color.TRANSPARENT)
            for(y in 1 until 31)for(x in 1 until 38)setPixel(x,y,Color.argb(220,220,45,60))
        }
        val mask=Bitmap.createBitmap(39,33,Bitmap.Config.ARGB_8888).apply{
            for(y in 0 until height)for(x in 0 until width){val v=if(x<20)255 else 120;setPixel(x,y,Color.argb(255,v,v,v))}
        }
        val top=Bitmap.createBitmap(39,33,Bitmap.Config.ARGB_8888).apply{
            eraseColor(Color.TRANSPARENT)
            for(y in 7 until 25)for(x in 9 until 30)setPixel(x,y,Color.argb(200,40,210,110))
        }
        val layers=listOf(
            EditorLayer(name="Base",bitmap=base,opacity=.8f),
            EditorLayer(name="Clipped 1",bitmap=clipped,opacity=.7f,mask=mask,clipToBelow=true),
            EditorLayer(name="Clipped 2",bitmap=top,opacity=.6f,clipToBelow=true)
        )
        val reference=com.winsmediatama.winsphotostudio.core.editor.LayerCompositeRenderer.render(layers,39,33)
        val stream=java.io.ByteArrayOutputStream()
        try{
            TiledLayerCompositeRenderer.encodeStreamingClippedOperationLayers(layers,39,33,stream,tileSize=32)
            val decoded=requireNotNull(android.graphics.BitmapFactory.decodeByteArray(stream.toByteArray(),0,stream.size()))
            for(y in 0 until 33)for(x in 0 until 39)
                assertEquals("clip streaming pixel ($x,$y)",reference.getPixel(x,y),decoded.getPixel(x,y))
            decoded.recycle()
        }finally{reference.recycle();base.recycle();clipped.recycle();mask.recycle();top.recycle()}
    }

    @Test fun streamedGroupCompositingMatchesFullRenderer() {
        val base=Bitmap.createBitmap(40,35,Bitmap.Config.ARGB_8888).apply {
            for(y in 0 until height)for(x in 0 until width)setPixel(x,y,Color.rgb((x*3)%255,(y*6)%255,80))
        }
        val child1=Bitmap.createBitmap(40,35,Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
            for(y in 3 until 31)for(x in 4 until 36)setPixel(x,y,Color.rgb(180,50,40))
        }
        val child2=Bitmap.createBitmap(40,35,Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
            for(y in 8 until 29)for(x in 10 until 30)setPixel(x,y,Color.rgb(35,160,210))
        }
        val mask=Bitmap.createBitmap(40,35,Bitmap.Config.ARGB_8888).apply {
            for(y in 0 until height)for(x in 0 until width){val v=if(x<20)255 else 128;setPixel(x,y,Color.argb(255,v,v,v))}
        }
        val layers=listOf(
            EditorLayer(name="Background",bitmap=base),
            EditorLayer(name="Group Red",bitmap=child1,groupId="g1",groupName="Effects",opacity=.72f,groupOpacity=.58f,groupBlendMode=com.winsmediatama.winsphotostudio.core.editor.EditorBlendMode.SCREEN),
            EditorLayer(name="Group Blue",bitmap=child2,groupId="g1",groupName="Effects",opacity=.83f,mask=mask,groupOpacity=.58f,groupBlendMode=com.winsmediatama.winsphotostudio.core.editor.EditorBlendMode.SCREEN)
        )
        val reference=com.winsmediatama.winsphotostudio.core.editor.LayerCompositeRenderer.render(layers,40,35)
        val stream=java.io.ByteArrayOutputStream()
        try{
            TiledLayerCompositeRenderer.encodeStreamingGroupedOperationLayers(layers,40,35,stream,tileSize=32)
            val decoded=requireNotNull(android.graphics.BitmapFactory.decodeByteArray(stream.toByteArray(),0,stream.size()))
            for(y in 0 until 35)for(x in 0 until 40)
                assertEquals("group streaming pixel ($x,$y)",reference.getPixel(x,y),decoded.getPixel(x,y))
            decoded.recycle()
        }finally{reference.recycle();base.recycle();child1.recycle();child2.recycle();mask.recycle()}
    }

    @Test fun streamedMultiLayerOperationReplayMatchesFullComposite() {
        val base = Bitmap.createBitmap(45, 37, Bitmap.Config.ARGB_8888).apply {
            for(y in 0 until height) for(x in 0 until width) setPixel(x,y,Color.rgb((x*5)%256,(y*7)%256,90))
        }
        val overlay = Bitmap.createBitmap(45, 37, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
            for(y in 4 until 34) for(x in 6 until 39) setPixel(x,y,Color.rgb(210,45,75))
        }
        val mask = Bitmap.createBitmap(45, 37, Bitmap.Config.ARGB_8888).apply {
            for(y in 0 until height) for(x in 0 until width) {
                val value=if(x<width/2) 255 else 100
                setPixel(x,y,Color.argb(255,value,value,value))
            }
        }
        val adjustment=com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues(brightness=.08f,contrast=.04f,saturation=.1f)
        val operation=com.winsmediatama.winsphotostudio.core.history.EditOperation.BasicAdjust(values=adjustment)
        val layers=listOf(
            EditorLayer(name="Base",bitmap=base),
            EditorLayer(name="Edited Overlay",bitmap=overlay,opacity=.8f,mask=mask,operations=listOf(operation))
        )
        val reference=com.winsmediatama.winsphotostudio.core.editor.LayerCompositeRenderer.render(layers,45,37)
        val stream=java.io.ByteArrayOutputStream()
        try {
            TiledLayerCompositeRenderer.encodeStreamingOperationLayers(layers,45,37,stream,tileSize=32)
            val decoded=requireNotNull(android.graphics.BitmapFactory.decodeByteArray(stream.toByteArray(),0,stream.size()))
            for(y in 0 until 37) for(x in 0 until 45)
                assertEquals("multi-layer replay pixel ($x,$y)",reference.getPixel(x,y),decoded.getPixel(x,y))
            decoded.recycle()
        } finally { reference.recycle();base.recycle();overlay.recycle();mask.recycle() }
    }

    @Test fun streamingScanlineEncoderHonorsCancellationCallback() {
        val bitmap = Bitmap.createBitmap(3, 3, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val layers = listOf(EditorLayer(name = "Layer", bitmap = bitmap))
        val stream = java.io.ByteArrayOutputStream()
        var rows = 0
        try {
            TiledLayerCompositeRenderer.encodeStreamingPng(layers, 3, 3, stream, tileWidth = 2, cancelCheck = {
                if (rows >= 1) throw kotlinx.coroutines.CancellationException("test cancellation") else rows++
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
