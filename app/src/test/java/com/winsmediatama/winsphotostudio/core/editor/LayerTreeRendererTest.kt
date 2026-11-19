package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class LayerTreeRendererTest {
    @Test fun groupRendersChildrenAndVisibility() {
        val red=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val tree=listOf(LayerNode.GroupNode(name="Visible group",children=listOf(LayerNode.BitmapNode(EditorLayer(name="Red",bitmap=red)))))
        assertEquals(Color.RED,LayerTreeRenderer.render(tree,1,1).getPixel(0,0))
        assertEquals(Color.TRANSPARENT,LayerTreeRenderer.render(tree.map{(it as LayerNode.GroupNode).copy(visible=false)},1,1).getPixel(0,0))
    }
    @Test fun groupOpacityAffectsRenderedAlpha() {
        val bitmap=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val node=LayerNode.GroupNode(children=listOf(LayerNode.BitmapNode(EditorLayer(name="Layer",bitmap=bitmap))),opacity=.5f)
        assertEquals(127,Color.alpha(LayerTreeRenderer.render(listOf(node),1,1).getPixel(0,0)))
    }
}
