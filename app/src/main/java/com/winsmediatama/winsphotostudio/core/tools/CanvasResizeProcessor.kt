package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

enum class CanvasAnchor { CENTER, TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT }

object CanvasResizeProcessor {
    fun resize(source:Bitmap,width:Int,height:Int,anchor:CanvasAnchor=CanvasAnchor.CENTER,backgroundColor:Int=android.graphics.Color.TRANSPARENT):Bitmap {
        require(width in 1..16384 && height in 1..16384){"Canvas dimensions must be 1..16384"}
        val output=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(output);canvas.drawColor(backgroundColor)
        val dx=when(anchor){CanvasAnchor.TOP_LEFT,CanvasAnchor.CENTER_LEFT,CanvasAnchor.BOTTOM_LEFT->0;CanvasAnchor.TOP_CENTER,CanvasAnchor.CENTER,CanvasAnchor.BOTTOM_CENTER->(width-source.width)/2;else->width-source.width}
        val dy=when(anchor){CanvasAnchor.TOP_LEFT,CanvasAnchor.TOP_CENTER,CanvasAnchor.TOP_RIGHT->0;CanvasAnchor.CENTER_LEFT,CanvasAnchor.CENTER,CanvasAnchor.CENTER_RIGHT->(height-source.height)/2;else->height-source.height}
        canvas.drawBitmap(source,dx.toFloat(),dy.toFloat(),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return output
    }
}
