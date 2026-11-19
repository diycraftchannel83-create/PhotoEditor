package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap

/** Applies a grayscale mask to layer alpha. White reveals; black hides; intermediate gray partially reveals. */
object LayerMaskProcessor {
    fun apply(source: Bitmap, mask: Bitmap): Bitmap {
        require(source.width > 0 && source.height > 0 && mask.width > 0 && mask.height > 0)
        val output=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
        val sourcePixels=IntArray(source.width*source.height)
        source.getPixels(sourcePixels,0,source.width,0,0,source.width,source.height)
        val maskPixels=IntArray(mask.width*mask.height)
        mask.getPixels(maskPixels,0,mask.width,0,0,mask.width,mask.height)
        for(y in 0 until source.height) for(x in 0 until source.width) {
            val i=y*source.width+x
            val mx=(x.toFloat()/source.width*mask.width).toInt().coerceIn(0,mask.width-1)
            val my=(y.toFloat()/source.height*mask.height).toInt().coerceIn(0,mask.height-1)
            val m=maskPixels[my*mask.width+mx]
            val luminance=((m shr 16 and 255)*0.2126f+(m shr 8 and 255)*0.7152f+(m and 255)*0.0722f).toInt()
            val maskAlpha=m ushr 24
            val factor=luminance*maskAlpha/255f
            val p=sourcePixels[i]
            val alpha=((p ushr 24)*factor/255f).toInt().coerceIn(0,255)
            sourcePixels[i]=(alpha shl 24) or (p and 0x00FFFFFF)
        }
        output.setPixels(sourcePixels,0,source.width,0,0,source.width,source.height)
        return output
    }
    fun createWhiteMask(width:Int,height:Int):Bitmap=Bitmap.createBitmap(width.coerceAtLeast(1),height.coerceAtLeast(1),Bitmap.Config.ARGB_8888).apply{eraseColor(android.graphics.Color.WHITE)}
    fun invert(mask:Bitmap):Bitmap {
        val result=Bitmap.createBitmap(mask.width,mask.height,Bitmap.Config.ARGB_8888)
        val pixels=IntArray(mask.width*mask.height);mask.getPixels(pixels,0,mask.width,0,0,mask.width,mask.height)
        for(i in pixels.indices){val p=pixels[i];pixels[i]=(p and 0xFF000000) or ((255-(p shr 16 and 255)) shl 16) or ((255-(p shr 8 and 255)) shl 8) or (255-(p and 255))}
        result.setPixels(pixels,0,mask.width,0,0,mask.width,mask.height)
        return result
    }
}
