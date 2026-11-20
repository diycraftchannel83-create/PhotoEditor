package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.CancellationSignal

data class PerspectiveQuad(val topLeft:Pair<Float,Float>,val topRight:Pair<Float,Float>,val bottomRight:Pair<Float,Float>,val bottomLeft:Pair<Float,Float>) {
    fun isValid():Boolean {
        val pts=listOf(topLeft,topRight,bottomRight,bottomLeft)
        if(pts.any{it.first !in 0f..1f||it.second !in 0f..1f})return false
        val cross=mutableListOf<Float>()
        for(i in pts.indices){val a=pts[i];val b=pts[(i+1)%4];val c=pts[(i+2)%4];cross+=((b.first-a.first)*(c.second-b.second)-(b.second-a.second)*(c.first-b.first))}
        return cross.all{it>0f}||cross.all{it<0f}
    }
    companion object {val IDENTITY=PerspectiveQuad(0f to 0f,1f to 0f,1f to 1f,0f to 1f)}
}
object PerspectiveTransformProcessor {
    fun warp(source:Bitmap,quad:PerspectiveQuad,outputWidth:Int=source.width,outputHeight:Int=source.height,cancellation:CancellationSignal?=null):Bitmap {
        cancellation?.throwIfCanceled()
        require(quad.isValid()){"Perspective quadrilateral must be valid and convex"}
        val w=outputWidth.coerceIn(1,8192);val h=outputHeight.coerceIn(1,8192)
        require(w.toLong()*h.toLong()<=24_000_000L){"Perspective output exceeds 24 MP safety limit"}
        if(quad==PerspectiveQuad.IDENTITY&&w==source.width&&h==source.height)return source.copy(Bitmap.Config.ARGB_8888,true)
        val src=floatArrayOf(quad.topLeft.first*source.width,quad.topLeft.second*source.height,quad.topRight.first*source.width,quad.topRight.second*source.height,quad.bottomRight.first*source.width,quad.bottomRight.second*source.height,quad.bottomLeft.first*source.width,quad.bottomLeft.second*source.height)
        val dst=floatArrayOf(0f,0f,w.toFloat(),0f,w.toFloat(),h.toFloat(),0f,h.toFloat())
        val matrix=Matrix();require(matrix.setPolyToPoly(src,0,dst,0,4) ){"Unable to compute perspective transform"}
        val output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        try{Canvas(output).drawBitmap(source,matrix,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG));cancellation?.throwIfCanceled();return output}catch(error:Throwable){if(!output.isRecycled)output.recycle();throw error}
    }
}
