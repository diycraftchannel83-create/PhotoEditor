package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

data class TransformSpec(
    val left:Float=0f,val top:Float=0f,val right:Float=1f,val bottom:Float=1f,
    val rotationDegrees:Float=0f,val flipHorizontal:Boolean=false,val flipVertical:Boolean=false,
    val outputWidth:Int=0,val outputHeight:Int=0
) {
    fun isValid():Boolean=left in 0f..1f&&top in 0f..1f&&right in 0f..1f&&bottom in 0f..1f&&left<right&&top<bottom&&rotationDegrees.isFinite()
}

object TransformProcessor {
    fun apply(source:Bitmap,spec:TransformSpec):Bitmap {
        require(spec.isValid()){"Transform bounds are invalid"}
        val left=floor(spec.left*source.width).toInt().coerceIn(0,source.width-1)
        val top=floor(spec.top*source.height).toInt().coerceIn(0,source.height-1)
        val right=ceil(spec.right*source.width).toInt().coerceIn(left+1,source.width)
        val bottom=ceil(spec.bottom*source.height).toInt().coerceIn(top+1,source.height)
        val cropW=right-left;val cropH=bottom-top
        val cropPixels=IntArray(cropW*cropH)
        source.getPixels(cropPixels,0,cropW,left,top,cropW,cropH)

        // Flip the cropped pixels explicitly so results do not depend on Android Matrix ordering.
        val flipped=IntArray(cropPixels.size)
        for(y in 0 until cropH) for(x in 0 until cropW) {
            val sx=if(spec.flipHorizontal) cropW-1-x else x
            val sy=if(spec.flipVertical) cropH-1-y else y
            flipped[y*cropW+x]=cropPixels[sy*cropW+sx]
        }

        val angle=((spec.rotationDegrees%360f)+360f)%360f
        val cardinal=when {
            abs(angle-0f)<0.001f||abs(angle-360f)<0.001f -> 0
            abs(angle-90f)<0.001f -> 90
            abs(angle-180f)<0.001f -> 180
            abs(angle-270f)<0.001f -> 270
            else -> -1
        }
        var transformed:Bitmap
        if(cardinal>=0) {
            val outW=if(cardinal==90||cardinal==270)cropH else cropW
            val outH=if(cardinal==90||cardinal==270)cropW else cropH
            val rotated=IntArray(outW*outH)
            for(y in 0 until cropH) for(x in 0 until cropW) {
                val (dx,dy)=when(cardinal){
                    0 -> x to y
                    90 -> (cropH-1-y) to x
                    180 -> (cropW-1-x) to (cropH-1-y)
                    else -> y to (cropW-1-x)
                }
                rotated[dy*outW+dx]=flipped[y*cropW+x]
            }
            transformed=Bitmap.createBitmap(outW,outH,Bitmap.Config.ARGB_8888)
            transformed.setPixels(rotated,0,outW,0,0,outW,outH)
        } else {
            val crop=Bitmap.createBitmap(cropW,cropH,Bitmap.Config.ARGB_8888).apply{setPixels(flipped,0,cropW,0,0,cropW,cropH)}
            val matrix=Matrix().apply{setRotate(spec.rotationDegrees)}
            transformed=Bitmap.createBitmap(crop,0,0,crop.width,crop.height,matrix,true)
            if(transformed!==crop&&!crop.isRecycled)crop.recycle()
        }
        val outW=spec.outputWidth.takeIf{it>0}?:transformed.width
        val outH=spec.outputHeight.takeIf{it>0}?:transformed.height
        if(outW==transformed.width&&outH==transformed.height)return transformed
        val resized=Bitmap.createScaledBitmap(transformed,outW.coerceAtLeast(1),outH.coerceAtLeast(1),true)
        if(resized!==transformed&&!transformed.isRecycled)transformed.recycle()
        return resized
    }
}
