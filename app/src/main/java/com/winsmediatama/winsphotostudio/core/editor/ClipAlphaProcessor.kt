package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap

/** Applies the alpha channel of the layer below to the current layer, preserving current RGB. */
object ClipAlphaProcessor {
    fun apply(source:Bitmap,below:Bitmap):Bitmap {
        val w=source.width;val h=source.height
        val current=IntArray(w*h);source.getPixels(current,0,w,0,0,w,h)
        val base=Bitmap.createScaledBitmap(below,w,h,true)
        val alpha=IntArray(w*h);base.getPixels(alpha,0,w,0,0,w,h)
        if(base!==below)base.recycle()
        for(i in current.indices){val a=(current[i] ushr 24)*(alpha[i] ushr 24)/255;current[i]=(a shl 24) or (current[i] and 0x00FFFFFF)}
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(current,0,w,0,0,w,h)}
    }
}
