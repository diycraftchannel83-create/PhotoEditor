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
            val groupPaint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply{alpha=(group.firstOrNull()?.groupOpacity?.coerceIn(0f,1f)?:1f*255).toInt()}
            groupPaint.alpha=((group.firstOrNull()?.groupOpacity?:1f).coerceIn(0f,1f)*255).toInt()
            canvas.drawBitmap(groupBitmap,null,dest,groupPaint)
            groupBitmap.recycle()
        }
        return output
    }
    private fun drawLayer(canvas:Canvas,layer:EditorLayer,dest:Rect) {
        val masked=layer.mask?.takeUnless{it.isRecycled}?.let{LayerMaskProcessor.apply(layer.bitmap,it)}
        val source=masked?:layer.bitmap
        val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply{alpha=(layer.opacity.coerceIn(0f,1f)*255f).toInt()}
        if(layer.blendMode==EditorBlendMode.DIFFERENCE&&Build.VERSION.SDK_INT>=29){
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
