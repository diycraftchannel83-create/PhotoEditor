package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap

/** Applies the existing layer alpha to edited pixels, preventing new alpha or transparent-area changes. */
object AlphaLockProcessor {
    fun apply(original:Bitmap,edited:Bitmap):Bitmap {
        val w=edited.width;val h=edited.height
        val oldAlpha=IntArray(original.width*original.height);original.getPixels(oldAlpha,0,original.width,0,0,original.width,original.height)
        val pixels=IntArray(w*h);edited.getPixels(pixels,0,w,0,0,w,h)
        for(y in 0 until h)for(x in 0 until w){val i=y*w+x;val ox=(x.toFloat()/w*original.width).toInt().coerceIn(0,original.width-1);val oy=(y.toFloat()/h*original.height).toInt().coerceIn(0,original.height-1);val a=oldAlpha[oy*original.width+ox] ushr 24;pixels[i]=(a shl 24) or (pixels[i] and 0x00FFFFFF)}
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
    }
}
