package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.CancellationSignal
import java.util.UUID
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack

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

    /** Adjustment layer applies a replayable pixel adjustment to all layers below it. */
    data class AdjustmentNode(
        override val id:String=UUID.randomUUID().toString(),
        override val name:String="Adjustment",
        val operation:EditOperation,
        val mask:Bitmap?=null,
        override val visible:Boolean=true,
        override val opacity:Float=1f
    ):LayerNode {
        init {
            require(operation is EditOperation.BasicAdjust || operation is EditOperation.Levels ||
                operation is EditOperation.Curves || operation is EditOperation.Hsl ||
                operation is EditOperation.Effect || operation is EditOperation.Lut) {
                "Adjustment layers only accept pixel-color adjustment operations"
            }
            require(opacity.isFinite())
        }
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
    private const val MAX_PIXELS=24_000_000L
    private const val MAX_DEPTH=32

    private fun applyAdjustment(target:Bitmap,node:LayerNode.AdjustmentNode,cancellation:CancellationSignal?) {
        cancellation?.throwIfCanceled()
        val adjusted=NonDestructiveEditStack(target,listOf(node.operation)).render()
        try {
            require(adjusted.width==target.width && adjusted.height==target.height) {
                "Adjustment layer must preserve canvas dimensions"
            }
            val w=target.width; val h=target.height
            val mask=node.mask?.takeUnless{it.isRecycled}
            val stripRows=128
            for(top in 0 until h step stripRows) {
                cancellation?.throwIfCanceled()
                val rows=minOf(stripRows,h-top)
                val count=w*rows
                val basePixels=IntArray(count)
                val adjustedPixels=IntArray(count)
                target.getPixels(basePixels,0,w,0,top,w,rows)
                adjusted.getPixels(adjustedPixels,0,w,0,top,w,rows)
                val maskTop=mask?.let { (top.toFloat()/h*it.height).toInt().coerceIn(0,it.height-1) } ?: 0
                val maskBottom=mask?.let { ((top+rows).toFloat()/h*it.height).toInt().coerceAtLeast(maskTop+1).coerceAtMost(it.height) } ?: 0
                val maskRows=(maskBottom-maskTop).coerceAtLeast(1)
                val maskPixels=mask?.let { IntArray(it.width*maskRows).also { p->it.getPixels(p,0,it.width,0,maskTop,it.width,maskRows) } }
                for(local in 0 until count) {
                    if(local and 0x3FFF==0) cancellation?.throwIfCanceled()
                    val x=local%w; val y=top+local/w
                    val maskValue=if(maskPixels==null) 1f else {
                        val mx=(x.toFloat()/w*mask.width).toInt().coerceIn(0,mask.width-1)
                        val my=(y.toFloat()/h*mask.height).toInt().coerceIn(0,mask.height-1)
                        val maskRowOffset=(my-maskTop).coerceIn(0,maskRows-1)
                        val m=maskPixels[maskRowOffset*mask.width+mx]
                        ((m ushr 16 and 255)/255f)*((m ushr 24 and 255)/255f)
                    }
                    val amount=node.opacity.coerceIn(0f,1f)*maskValue
                    if(amount<=0f)continue
                    val a=basePixels[local];val b=adjustedPixels[local]
                    fun blendShift(shift:Int)=(((a ushr shift)and 255)*(1f-amount)+((b ushr shift)and 255)*amount).toInt().coerceIn(0,255)
                    basePixels[local]=(blendShift(24) shl 24) or (blendShift(16) shl 16) or (blendShift(8) shl 8) or blendShift(0)
                }
                target.setPixels(basePixels,0,w,0,top,w,rows)
            }
        } finally { if(!adjusted.isRecycled)adjusted.recycle() }
    }

    fun render(nodes:List<LayerNode>,width:Int,height:Int,cancellation:CancellationSignal?=null):Bitmap=renderInternal(nodes,width,height,0,cancellation)

    private fun renderInternal(nodes:List<LayerNode>,width:Int,height:Int,depth:Int,cancellation:CancellationSignal?):Bitmap {
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1)
        require(w.toLong()*h.toLong()<=MAX_PIXELS) { "Layer tree render exceeds 24 MP safety limit (${w}x${h})" }
        require(depth<=MAX_DEPTH) { "Layer tree nesting exceeds maximum depth ($MAX_DEPTH)" }
        cancellation?.throwIfCanceled()
        val out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(out)
            val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            nodes.forEach { node ->
                if(!node.visible || node.opacity<=0f) return@forEach
                if(node is LayerNode.AdjustmentNode) {
                    applyAdjustment(out,node,cancellation)
                    return@forEach
                }
                val rendered=when(node){
                    is LayerNode.BitmapNode -> LayerCompositeRenderer.render(listOf(node.layer.copy(visible=true,opacity=1f)),w,h)
                    is LayerNode.GroupNode -> renderInternal(node.children,w,h,depth+1,cancellation)
                    is LayerNode.AdjustmentNode -> error("Adjustment node handled above")
                }
                try {
                    paint.alpha=(node.opacity.coerceIn(0f,1f)*255f).toInt()
                    canvas.drawBitmap(rendered,0f,0f,paint)
                } finally {
                    if(!rendered.isRecycled)rendered.recycle()
                    cancellation?.throwIfCanceled()
                }
            }
            return out
        } catch(error:Throwable) {
            if(!out.isRecycled)out.recycle()
            throw error
        }
    }
}
