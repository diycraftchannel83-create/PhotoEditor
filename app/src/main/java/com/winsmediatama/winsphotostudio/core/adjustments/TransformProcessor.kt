package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect

/** Crop, rotate/straighten, flip and resize processor used by replayable transform operations. */
object TransformProcessor {
    fun apply(source: Bitmap, left:Float, top:Float, right:Float, bottom:Float, rotation:Float, flipH:Boolean, flipV:Boolean, outputWidth:Int=0, outputHeight:Int=0):Bitmap {
        require(left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() && rotation.isFinite())
        require(left>=0f && top>=0f && right<=1f && bottom<=1f && left<right && top<bottom)
        val l=(left*source.width).toInt().coerceIn(0,source.width-1)
        val t=(top*source.height).toInt().coerceIn(0,source.height-1)
        val r=(right*source.width).toInt().coerceIn(l+1,source.width)
        val b=(bottom*source.height).toInt().coerceIn(t+1,source.height)
        var current=Bitmap.createBitmap(source,l,t,r-l,b-t)
        if(rotation!=0f || flipH || flipV){
            val matrix=Matrix().apply{setRotate(rotation);postScale(if(flipH)-1f else 1f,if(flipV)-1f else 1f)}
            val transformed=Bitmap.createBitmap(current,0,0,current.width,current.height,matrix,true)
            if(transformed!==current)current.recycle()
            current=transformed
        }
        val w=outputWidth.takeIf{it>0}?:current.width
        val h=outputHeight.takeIf{it>0}?:current.height
        if(current.width==w&&current.height==h)return current
        val resized=Bitmap.createBitmap(w.coerceAtLeast(1),h.coerceAtLeast(1),Bitmap.Config.ARGB_8888)
        Canvas(resized).drawBitmap(current,null,Rect(0,0,resized.width,resized.height),Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        current.recycle()
        return resized
    }
}
