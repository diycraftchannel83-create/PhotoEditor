package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect

object LayerCompositeRenderer {
    fun render(layers:List<EditorLayer>,width:Int,height:Int):Bitmap {
        val output=Bitmap.createBitmap(width.coerceAtLeast(1),height.coerceAtLeast(1),Bitmap.Config.ARGB_8888)
        val canvas=Canvas(output)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val dest=Rect(0,0,output.width,output.height)
        layers.filter { it.visible && !it.bitmap.isRecycled }.forEach { layer ->
            paint.alpha=(layer.opacity.coerceIn(0f,1f)*255f).toInt()
            paint.xfermode=PorterDuffXfermode(when(layer.blendMode) {
                EditorBlendMode.NORMAL->PorterDuff.Mode.SRC_OVER
                EditorBlendMode.MULTIPLY->PorterDuff.Mode.MULTIPLY
                EditorBlendMode.SCREEN->PorterDuff.Mode.SCREEN
                EditorBlendMode.OVERLAY->PorterDuff.Mode.OVERLAY
                EditorBlendMode.DARKEN->PorterDuff.Mode.DARKEN
                EditorBlendMode.LIGHTEN->PorterDuff.Mode.LIGHTEN
                EditorBlendMode.ADD->PorterDuff.Mode.ADD
                EditorBlendMode.DIFFERENCE->PorterDuff.Mode.XOR
            })
            canvas.drawBitmap(layer.bitmap,null,dest,paint)
        }
        paint.xfermode=null
        return output
    }
}
