package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/** Crop, rotate/straighten, flip and resize processor used by replayable transform operations. */
object TransformProcessor {
    fun apply(source: Bitmap, left:Float, top:Float, right:Float, bottom:Float, rotation:Float, flipH:Boolean, flipV:Boolean, outputWidth:Int=0, outputHeight:Int=0):Bitmap {
        require(source.width>0&&source.height>0)
        require(left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() && rotation.isFinite())
        require(left>=0f && top>=0f && right<=1f && bottom<=1f && left<right && top<bottom)
        val l=floor(left*source.width).toInt().coerceIn(0,source.width-1)
        val t=floor(top*source.height).toInt().coerceIn(0,source.height-1)
        val r=ceil(right*source.width).toInt().coerceIn(l+1,source.width)
        val b=ceil(bottom*source.height).toInt().coerceIn(t+1,source.height)
        val cropW=r-l;val cropH=b-t
        val crop=IntArray(cropW*cropH)
        source.getPixels(crop,0,cropW,l,t,cropW,cropH)
        val flipped=IntArray(crop.size)
        for(y in 0 until cropH) for(x in 0 until cropW) {
            val sx=if(flipH)cropW-1-x else x
            val sy=if(flipV)cropH-1-y else y
            flipped[y*cropW+x]=crop[sy*cropW+sx]
        }
        val normalized=((rotation%360f)+360f)%360f
        val quarter=when {
            abs(normalized)<0.001f||abs(normalized-360f)<0.001f -> 0
            abs(normalized-90f)<0.001f -> 90
            abs(normalized-180f)<0.001f -> 180
            abs(normalized-270f)<0.001f -> 270
            else -> -1
        }
        var transformed:Bitmap
        if(quarter>=0) {
            val outW=if(quarter==90||quarter==270)cropH else cropW
            val outH=if(quarter==90||quarter==270)cropW else cropH
            val pixels=IntArray(outW*outH)
            for(y in 0 until cropH) for(x in 0 until cropW) {
                val (dx,dy)=when(quarter) {
                    0 -> x to y
                    90 -> (cropH-1-y) to x
                    180 -> (cropW-1-x) to (cropH-1-y)
                    else -> y to (cropW-1-x)
                }
                pixels[dy*outW+dx]=flipped[y*cropW+x]
            }
            transformed=Bitmap.createBitmap(outW,outH,Bitmap.Config.ARGB_8888)
            transformed.setPixels(pixels,0,outW,0,0,outW,outH)
        } else {
            val cropBitmap=Bitmap.createBitmap(cropW,cropH,Bitmap.Config.ARGB_8888).apply{setPixels(flipped,0,cropW,0,0,cropW,cropH)}
            val matrix=Matrix().apply{setRotate(rotation)}
            transformed=Bitmap.createBitmap(cropBitmap,0,0,cropW,cropH,matrix,true)
            if(transformed!==cropBitmap&&!cropBitmap.isRecycled)cropBitmap.recycle()
        }
        val w=outputWidth.takeIf{it>0}?:transformed.width
        val h=outputHeight.takeIf{it>0}?:transformed.height
        if(transformed.width==w&&transformed.height==h)return transformed
        val resized=Bitmap.createScaledBitmap(transformed,w.coerceAtLeast(1),h.coerceAtLeast(1),true)
        if(resized!==transformed&&!transformed.isRecycled)transformed.recycle()
        return resized
    }
}
