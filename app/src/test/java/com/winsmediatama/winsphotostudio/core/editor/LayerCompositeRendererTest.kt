package com.winsmediatama.winsphotudio.core.editor

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class LayerCompositeRendererTest {
    @Test fun groupOpacityIsAppliedToCombinedLayers() {
        val red=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val blue=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLUE)}
        val group="g"
        val result=LayerCompositeRenderer.render(listOf(EditorLayer(name="R",bitmap=red,groupId=group,groupOpacity=.5f),EditorLayer(name="B",bitmap=blue,groupId=group,groupOpacity=.5f)),1,1)
        assertEquals(127,Color.alpha(result.getPixel(0,0)))
        assertEquals(255,Color.blue(result.getPixel(0,0)))
    }
    @Test fun maskedLayerCompositesTransparentOutsideMask() {
        val red=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val mask=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.WHITE);setPixel(1,0,Color.BLACK)}
        val result=LayerCompositeRenderer.render(listOf(EditorLayer(name="masked",bitmap=red,mask=mask)),2,1)
        assertEquals(255,Color.alpha(result.getPixel(0,0)))
        assertEquals(0,Color.alpha(result.getPixel(1,0)))
    }
}
