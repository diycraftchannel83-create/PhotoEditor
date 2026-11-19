package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.roundToInt

data class TransformSpec(val left:Float=0f,val top:Float=0f,val right:Float=1f,val bottom:Float=1f,val rotationDegrees:Float=0f,val flipHorizontal:Boolean=false,val flipVertical:Boolean=false,val outputWidth:Int=0,val outputHeight:Int=0) {
    fun isValid():Boolean=left in 0f..1f&&top in 0f..1f&&right in 0f..1f&&bottom in 0f..1f&&left<right&&top<bottom&&rotationDegrees.isFinite()
}
object TransformProcessor {
    fun apply(source:Bitmap,spec:TransformSpec):Bitmap {
        require(spec.isValid()){"Transform bounds are invalid"}
        val left=(spec.left*source.width).roundToInt().coerceIn(0,source.width-1)
        val top=(spec.top*source.height).roundToInt().coerceIn(0,source.height-1)
        val right=(spec.right*source.width).roundToInt().coerceIn(left+1,source.width)
        val bottom=(spec.bottom*source.height).roundToInt().coerceIn(top+1,source.height)
        val crop=Bitmap.createBitmap(source,left,top,right-left,bottom-top)
        val matrix=Matrix().apply{postRotate(spec.rotationDegrees);postScale(if(spec.flipHorizontal)-1f else 1f,if(spec.flipVertical)-1f else 1f)}
        val transformed=Bitmap.createBitmap(crop,0,0,crop.width,crop.height,matrix,true)
        if(transformed!==crop)crop.recycle()
        val outW=spec.outputWidth.takeIf{it>0}?:transformed.width
        val outH=spec.outputHeight.takeIf{it>0}?:transformed.height
        if(outW==transformed.width&&outH==transformed.height)return transformed
        val resized=Bitmap.createScaledBitmap(transformed,outW.coerceAtLeast(1),outH.coerceAtLeast(1),true)
        if(resized!==transformed)transformed.recycle()
        return resized
    }
}
