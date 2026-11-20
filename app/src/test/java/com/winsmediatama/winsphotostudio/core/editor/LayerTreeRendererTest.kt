package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Color
import android.os.CancellationSignal
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import com.winsmediatama.winsphotostudio.core.adjustments.LevelsChannel
import com.winsmediatama.winsphotostudio.core.adjustments.LevelsValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
    @Test fun adjustmentLayerChangesLayersBelowWithoutMutatingSource() {
        val source=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val operation=EditOperation.Levels(values=LevelsValues(composite=LevelsChannel(outputBlack=.5f,outputWhite=.5f)))
        val tree=listOf(
            LayerNode.BitmapNode(EditorLayer(name="Base",bitmap=source)),
            LayerNode.AdjustmentNode(name="Levels",operation=operation,opacity=1f)
        )
        val output=LayerTreeRenderer.render(tree,2,2)
        assertEquals(Color.RED,source.getPixel(0,0))
        assertEquals(false,output.getPixel(0,0)==Color.RED)
        output.recycle()
        source.recycle()
    }

    @Test fun blackAdjustmentMaskLeavesUnderlyingImageUnchanged() {
        val source=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val blackMask=Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLACK)}
        val operation=EditOperation.Levels(values=LevelsValues(composite=LevelsChannel(outputBlack=.5f,outputWhite=.5f)))
        val tree=listOf(
            LayerNode.BitmapNode(EditorLayer(name="Base",bitmap=source)),
            LayerNode.AdjustmentNode(name="Masked Levels",operation=operation,mask=blackMask)
        )
        val output=LayerTreeRenderer.render(tree,1,1)
        assertEquals(Color.RED,output.getPixel(0,0))
        output.recycle();source.recycle();blackMask.recycle()
    }

    @Test fun preCancelledLayerTreeRenderStopsBeforeAllocatingOutput() {\n        val signal=CancellationSignal().apply{cancel()}\n        assertThrows(android.os.OperationCanceledException::class.java) {\n            LayerTreeRenderer.render(emptyList(),2,2,signal)\n        }\n    }\n\n    @Test fun rejectsOversizedLayerTreeCanvasBeforeAllocation() {
        val error=assertThrows(IllegalArgumentException::class.java) {
            LayerTreeRenderer.render(emptyList(),6000,5000)
        }
        assertEquals(true,error.message?.contains("24 MP")==true)
    }

    @Test fun rejectsExcessiveGroupNesting() {
        var node:LayerNode=LayerNode.GroupNode()
        repeat(34){node=LayerNode.GroupNode(children=listOf(node))}
        val error=assertThrows(IllegalArgumentException::class.java) {
            LayerTreeRenderer.render(listOf(node),1,1)
        }
        assertEquals(true,error.message?.contains("nesting")==true)
    }
}
