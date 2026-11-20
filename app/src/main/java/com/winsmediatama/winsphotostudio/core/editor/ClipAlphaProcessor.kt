package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap

/** Applies the alpha channel of the layer below to the current layer, preserving current RGB. */
object ClipAlphaProcessor {
    private const val MAX_PIXELS=24_000_000L

    fun apply(source:Bitmap,below:Bitmap):Bitmap {
        val w=source.width;val h=source.height
        require(w>0&&h>0&&w.toLong()*h.toLong()<=MAX_PIXELS) { "Clip-alpha source exceeds 24 MP safety limit" }
        require(below.width>0&&below.height>0&&below.width.toLong()*below.height.toLong()<=MAX_PIXELS) { "Clip-alpha base exceeds 24 MP safety limit" }
        val current=IntArray(w*h)
        source.getPixels(current,0,w,0,0,w,h)
        val base=Bitmap.createScaledBitmap(below,w,h,true)
        val alpha=IntArray(w*h)
        try {
            base.getPixels(alpha,0,w,0,0,w,h)
        } finally {
            if(base!==below&&!base.isRecycled)base.recycle()
        }
        for(i in current.indices) {
            val a=(current[i] ushr 24)*(alpha[i] ushr 24)/255
            current[i]=(a shl 24) or (current[i] and 0x00FFFFFF)
        }
        val output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        try {
            output.setPixels(current,0,w,0,0,w,h)
            return output
        } catch(error:Throwable) {
            if(!output.isRecycled)output.recycle()
            throw error
        }
    }
}
