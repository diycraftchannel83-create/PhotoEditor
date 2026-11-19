package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import java.util.UUID

/** Tree model for nested layer groups. UI can flatten the tree for ordering while retaining group metadata. */
sealed interface LayerNode {
    val id:String
    val name:String
    val visible:Boolean
    val opacity:Float

    data class BitmapNode(
        val layer:EditorLayer,
        override val visible:Boolean=layer.visible,
        override val opacity:Float=layer.opacity
    ):LayerNode {
        override val id=layer.id
        override val name=layer.name
    }

    data class GroupNode(
        override val id:String=UUID.randomUUID().toString(),
        override val name:String="Group",
        val children:List<LayerNode> = emptyList(),
        override val visible:Boolean=true,
        override val opacity:Float=1f
    ):LayerNode
}

object LayerTreeRenderer {
    fun render(nodes:List<LayerNode>,width:Int,height:Int):Bitmap {
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1)
        val out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(out)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        nodes.forEach { node ->
            if(!node.visible || node.opacity<=0f) return@forEach
            val rendered=when(node){
                is LayerNode.BitmapNode -> LayerCompositeRenderer.render(listOf(node.layer.copy(visible=true,opacity=1f)),w,h)
                is LayerNode.GroupNode -> render(node.children,w,h)
            }
            paint.alpha=(node.opacity.coerceIn(0f,1f)*255f).toInt()
            canvas.drawBitmap(rendered,0f,0f,paint)
            if(!rendered.isRecycled)rendered.recycle()
        }
        return out
    }
}
