package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.Color

/** Applies a grayscale mask to layer alpha. White reveals; black hides; intermediate gray partially reveals. */
object LayerMaskProcessor {
    private const val MAX_PIXELS=24_000_000L

    fun apply(source: Bitmap, mask: Bitmap): Bitmap {
        require(source.width > 0 && source.height > 0 && mask.width > 0 && mask.height > 0)
        require(source.width.toLong()*source.height.toLong()<=MAX_PIXELS) { "Mask source exceeds 24 MP safety limit" }
        require(mask.width.toLong()*mask.height.toLong()<=MAX_PIXELS) { "Mask exceeds 24 MP safety limit" }
        val output=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
        try {
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
        } catch(error:Throwable) {
            if(!output.isRecycled)output.recycle()
            throw error
        }
    }

    /** Multiplies two reveal masks; white keeps and black removes. Caller-owned inputs are never recycled. */
    fun combineMultiply(first:Bitmap,second:Bitmap):Bitmap {
        val w=first.width;val h=first.height
        require(w>0&&h>0&&w.toLong()*h.toLong()<=MAX_PIXELS) { "Combined mask exceeds 24 MP safety limit" }
        require(second.width>0&&second.height>0&&second.width.toLong()*second.height.toLong()<=MAX_PIXELS) { "Input mask exceeds 24 MP safety limit" }
        var a:Bitmap?=null;var b:Bitmap?=null;var result:Bitmap?=null
        try {
            a=Bitmap.createScaledBitmap(first,w,h,true)
            b=Bitmap.createScaledBitmap(second,w,h,true)
            val ap=IntArray(w*h);val bp=IntArray(w*h)
            a.getPixels(ap,0,w,0,0,w,h);b.getPixels(bp,0,w,0,0,w,h)
            val out=IntArray(w*h){i->val av=ap[i] and 255;val bv=bp[i] and 255;val v=av*bv/255;Color.argb(255,v,v,v)}
            val created=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
            result=created
            created.setPixels(out,0,w,0,0,w,h)
            return created
        } catch(error:Throwable) {
            result?.takeUnless{it.isRecycled}?.recycle()
            throw error
        } finally {
            a?.takeIf{it!==first&&!it.isRecycled}?.recycle()
            b?.takeIf{it!==second&&!it.isRecycled}?.recycle()
        }
    }

    fun createWhiteMask(width:Int,height:Int):Bitmap {
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1)
        require(w.toLong()*h.toLong()<=MAX_PIXELS) { "White mask exceeds 24 MP safety limit" }
        val mask=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        try { mask.eraseColor(Color.WHITE);return mask }
        catch(error:Throwable){if(!mask.isRecycled)mask.recycle();throw error}
    }

    fun invert(mask:Bitmap):Bitmap {
        require(mask.width>0&&mask.height>0&&mask.width.toLong()*mask.height.toLong()<=MAX_PIXELS) { "Mask inversion exceeds 24 MP safety limit" }
        val result=Bitmap.createBitmap(mask.width,mask.height,Bitmap.Config.ARGB_8888)
        try {
            val pixels=IntArray(mask.width*mask.height)
            mask.getPixels(pixels,0,mask.width,0,0,mask.width,mask.height)
            for(i in pixels.indices){val p=pixels[i];pixels[i]=(p and 0xFF000000) or ((255-(p shr 16 and 255)) shl 16) or ((255-(p shr 8 and 255)) shl 8) or (255-(p and 255))}
            result.setPixels(pixels,0,mask.width,0,0,mask.width,mask.height)
            return result
        } catch(error:Throwable) {
            if(!result.isRecycled)result.recycle()
            throw error
        }
    }
}
