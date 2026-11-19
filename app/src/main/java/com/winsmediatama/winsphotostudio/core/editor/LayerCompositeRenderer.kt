package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.os.Build
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect

object LayerCompositeRenderer {
    fun render(layers:List<EditorLayer>,width:Int,height:Int):Bitmap {
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1)
        val output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(output);val dest=Rect(0,0,w,h)
        var index=0
        while(index<layers.size){
            val layer=layers[index]
            if(layer.groupId.isNullOrBlank()){
                if(layer.visible&&!layer.bitmap.isRecycled)drawLayer(canvas,layer,dest)
                index++
                continue
            }
            val groupId=layer.groupId
            val group=mutableListOf<EditorLayer>()
            while(index<layers.size&&layers[index].groupId==groupId){group+=layers[index];index++}
            val groupBitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
            val groupCanvas=Canvas(groupBitmap)
            group.filter{it.visible&&!it.bitmap.isRecycled}.forEach{child->drawLayer(groupCanvas,child.copy(groupId=null,groupOpacity=1f),dest)}
            val groupPaint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            groupPaint.alpha=((group.firstOrNull()?.groupOpacity?:1f).coerceIn(0f,1f)*255).toInt()
            canvas.drawBitmap(groupBitmap,null,dest,groupPaint)
            groupBitmap.recycle()
        }
        return output
    }
    private fun drawDifference(canvas:Canvas,source:Bitmap,dest:Rect,opacity:Int) {
        val target=canvas.bitmap ?: return
        val w=target.width;val h=target.height
        val base=IntArray(w*h);target.getPixels(base,0,w,0,0,w,h)
        val src=Bitmap.createScaledBitmap(source,w,h,true)
        val pixels=IntArray(w*h);src.getPixels(pixels,0,w,0,0,w,h);src.recycle()
        for(i in base.indices){
            val d=base[i];val s=pixels[i]
            val da=d ushr 24;val sa=((s ushr 24)*opacity/255)
            val outA=sa+da*(255-sa)/255
            if(outA<=0){base[i]=0;continue}
            val dr=(d shr 16)and 255;val dg=(d shr 8)and 255;val db=d and 255
            val sr=(s shr 16)and 255;val sg=(s shr 8)and 255;val sb=s and 255
            val r=(dr*(255-sa)+kotlin.math.abs(dr-sr)*sa)/255
            val g=(dg*(255-sa)+kotlin.math.abs(dg-sg)*sa)/255
            val b=(db*(255-sa)+kotlin.math.abs(db-sb)*sa)/255
            base[i]=(outA shl 24)or(r shl 16)or(g shl 8)or b
        }
        target.setPixels(base,0,w,0,0,w,h)
    }
    private fun drawLayer(canvas:Canvas,layer:EditorLayer,dest:Rect) {
        val masked=layer.mask?.takeUnless{it.isRecycled}?.let{LayerMaskProcessor.apply(layer.bitmap,it)}
        val source=masked?:layer.bitmap
        val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply{alpha=(layer.opacity.coerceIn(0f,1f)*255f).toInt()}
        if(layer.blendMode==EditorBlendMode.DIFFERENCE&&Build.VERSION.SDK_INT<29){\n            drawDifference(canvas,source,dest,paint.alpha)\n            if(masked!=null&&!masked.isRecycled)masked.recycle()\n            return\n        }\n        if(layer.blendMode==EditorBlendMode.DIFFERENCE&&Build.VERSION.SDK_INT>=29){
            paint.xfermode=null;paint.blendMode=BlendMode.DIFFERENCE
        }else{
            paint.blendMode=null
            paint.xfermode=PorterDuffXfermode(when(layer.blendMode){
                EditorBlendMode.NORMAL->PorterDuff.Mode.SRC_OVER
                EditorBlendMode.MULTIPLY->PorterDuff.Mode.MULTIPLY
                EditorBlendMode.SCREEN->PorterDuff.Mode.SCREEN
                EditorBlendMode.OVERLAY->PorterDuff.Mode.OVERLAY
                EditorBlendMode.DARKEN->PorterDuff.Mode.DARKEN
                EditorBlendMode.LIGHTEN->PorterDuff.Mode.LIGHTEN
                EditorBlendMode.ADD->PorterDuff.Mode.ADD
                EditorBlendMode.DIFFERENCE->PorterDuff.Mode.XOR
            })
        }
        canvas.drawBitmap(source,null,dest,paint)
        if(masked!=null&&!masked.isRecycled)masked.recycle()
    }
}
