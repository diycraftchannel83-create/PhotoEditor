package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.Color
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LayerRenderCacheKeyTest {
    @Test fun keyIsDeterministicForUnchangedStack() {
        val bitmap=Bitmap.createBitmap(8,6,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.WHITE)}
        try{
            val layers=listOf(EditorLayer(id="layer-a",name="A",bitmap=bitmap))
            assertEquals(LayerRenderCacheKey.create(layers,8,6),LayerRenderCacheKey.create(layers,8,6))
        }finally{bitmap.recycle()}
    }

    @Test fun keyInvalidatesOnBitmapGenerationChange() {
        val bitmap=Bitmap.createBitmap(8,6,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.WHITE)}
        try{
            val layers=listOf(EditorLayer(id="layer-a",name="A",bitmap=bitmap))
            val before=LayerRenderCacheKey.create(layers,8,6)
            bitmap.setPixel(2,3,Color.RED)
            val after=LayerRenderCacheKey.create(layers,8,6)
            assertNotEquals(before,after)
        }finally{bitmap.recycle()}
    }

    @Test fun keyInvalidatesOnLayerPropertiesAndCanvasSize() {
        val bitmap=Bitmap.createBitmap(8,6,Bitmap.Config.ARGB_8888)
        try{
            val original=EditorLayer(id="layer-a",name="A",bitmap=bitmap)
            val base=LayerRenderCacheKey.create(listOf(original),8,6)
            assertNotEquals(base,LayerRenderCacheKey.create(listOf(original.copy(opacity=.5f)),8,6))
            assertNotEquals(base,LayerRenderCacheKey.create(listOf(original),9,6))
            assertNotEquals(base,LayerRenderCacheKey.create(listOf(original.copy(visible=false)),8,6))
        }finally{bitmap.recycle()}
    }
}
