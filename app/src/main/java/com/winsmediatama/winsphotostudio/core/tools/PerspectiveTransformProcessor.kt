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
    private fun solveHomography(src:FloatArray,dst:FloatArray):FloatArray {
        // Solve the 8 homography parameters using pivoted Gaussian elimination.
        val a=Array(8){DoubleArray(9)}
        for(i in 0 until 4){
            val x=src[i*2].toDouble();val y=src[i*2+1].toDouble()
            val u=dst[i*2].toDouble();val v=dst[i*2+1].toDouble()
            val r=i*2
            a[r][0]=x;a[r][1]=y;a[r][2]=1.0;a[r][6]=-u*x;a[r][7]=-u*y;a[r][8]=u
            a[r+1][3]=x;a[r+1][4]=y;a[r+1][5]=1.0;a[r+1][6]=-v*x;a[r+1][7]=-v*y;a[r+1][8]=v
        }
        for(col in 0 until 8){
            var pivot=col
            for(row in col+1 until 8)if(kotlin.math.abs(a[row][col])>kotlin.math.abs(a[pivot][col]))pivot=row
            if(kotlin.math.abs(a[pivot][col])<1e-10)throw IllegalArgumentException("Perspective points are degenerate")
            if(pivot!=col){val temp=a[pivot];a[pivot]=a[col];a[col]=temp}
            val divisor=a[col][col]
            for(j in col until 9)a[col][j]/=divisor
            for(row in 0 until 8)if(row!=col){
                val factor=a[row][col]
                if(factor!=0.0)for(j in col until 9)a[row][j]-=factor*a[col][j]
            }
        }
        return floatArrayOf(a[0][8].toFloat(),a[1][8].toFloat(),a[2][8].toFloat(),a[3][8].toFloat(),a[4][8].toFloat(),a[5][8].toFloat(),a[6][8].toFloat(),a[7][8].toFloat(),1f)
    }

    fun warp(source:Bitmap,quad:PerspectiveQuad,outputWidth:Int=source.width,outputHeight:Int=source.height,cancellation:CancellationSignal?=null):Bitmap {
        cancellation?.throwIfCanceled()
        require(quad.isValid()){"Perspective quadrilateral must be valid and convex"}
        val w=outputWidth.coerceIn(1,8192);val h=outputHeight.coerceIn(1,8192)
        require(w.toLong()*h.toLong()<=24_000_000L){"Perspective output exceeds 24 MP safety limit"}
        if(quad==PerspectiveQuad.IDENTITY&&w==source.width&&h==source.height)return source.copy(Bitmap.Config.ARGB_8888,true)
        val src=floatArrayOf(quad.topLeft.first*source.width,quad.topLeft.second*source.height,quad.topRight.first*source.width,quad.topRight.second*source.height,quad.bottomRight.first*source.width,quad.bottomRight.second*source.height,quad.bottomLeft.first*source.width,quad.bottomLeft.second*source.height)
        val dst=floatArrayOf(0f,0f,w.toFloat(),0f,w.toFloat(),h.toFloat(),0f,h.toFloat())
        val matrix=Matrix().apply{setValues(solveHomography(src,dst))}
        val output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        try{Canvas(output).drawBitmap(source,matrix,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG));cancellation?.throwIfCanceled();return output}catch(error:Throwable){if(!output.isRecycled)output.recycle();throw error}
    }
}
