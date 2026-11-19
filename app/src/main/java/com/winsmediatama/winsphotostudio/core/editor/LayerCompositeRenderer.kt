package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect\nimport android.graphics.PorterDuff\nimport android.graphics.PorterDuffXfermode

object LayerCompositeRenderer {
    fun render(layers:List<EditorLayer>,width:Int,height:Int):Bitmap {
        val output=Bitmap.createBitmap(width.coerceAtLeast(1),height.coerceAtLeast(1),Bitmap.Config.ARGB_8888)
        val canvas=Canvas(output)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val dest=Rect(0,0,output.width,output.height)
        layers.filter { it.visible && !it.bitmap.isRecycled }.forEach { layer ->
            paint.alpha=(layer.opacity.coerceIn(0f,1f)*255f).toInt()
            canvas.drawBitmap(layer.bitmap,null,dest,paint)
        }
        return output
    }
}
