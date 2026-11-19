package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class LayerCompositeRendererTest {
    @Test @Config(sdk=[28]) fun differenceBlendProducesChannelDifferenceOnApi28() {
        val blue=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLUE)}
        val red=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val result=LayerCompositeRenderer.render(listOf(EditorLayer(name="Base",bitmap=blue),EditorLayer(name="Difference",bitmap=red,blendMode=EditorBlendMode.DIFFERENCE)),1,1)
        val pixel=result.getPixel(0,0)
        assertEquals(255,Color.red(pixel));assertEquals(0,Color.green(pixel));assertEquals(255,Color.blue(pixel))
    }
    @Test fun clippingMaskRestrictsUpperLayerToLowerAlpha() {
        val base=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888).apply{setPixel(0,0,Color.BLUE);setPixel(1,0,Color.TRANSPARENT)}
        val top=Bitmap.createBitmap(2,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val result=LayerCompositeRenderer.render(listOf(EditorLayer(name="Base",bitmap=base),EditorLayer(name="Clip",bitmap=top,clipToBelow=true)),2,1)
        assertEquals(Color.RED,result.getPixel(0,0))
        assertEquals(0,Color.alpha(result.getPixel(1,0)))
    }
    @Test fun clippingUsesBelowLayerOpacity() {
        val base=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLUE)}
        val top=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val result=LayerCompositeRenderer.render(listOf(EditorLayer(name="Base",bitmap=base,opacity=.5f),EditorLayer(name="Clip",bitmap=top,clipToBelow=true)),1,1)
        assertEquals(191,Color.alpha(result.getPixel(0,0)))
    }
    @Test fun groupBlendModeAppliesToCombinedGroup() {
        val base=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val cyan=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.CYAN)}
        val group=EditorLayer(name="Grouped",bitmap=cyan,groupId="g",groupName="G",groupOpacity=1f,groupBlendMode=EditorBlendMode.MULTIPLY)
        val result=LayerCompositeRenderer.render(listOf(EditorLayer(name="Base",bitmap=base),group),1,1)
        assertEquals(Color.BLACK,result.getPixel(0,0))
    }
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
