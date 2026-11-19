package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/** CPU effects for future effect-stack integration. */
object EffectProcessor {
    fun gaussianBlur(source:Bitmap,radius:Int):Bitmap {
        val r=radius.coerceIn(0,24);if(r==0)return source.copy(Bitmap.Config.ARGB_8888,true)
        val w=source.width;val h=source.height;val src=IntArray(w*h);source.getPixels(src,0,w,0,0,w,h)
        val sigma=max(.5f,r/2f);val kernel=FloatArray(2*r+1);var total=0f
        for(i in -r..r){val v=exp(-(i*i)/(2*sigma*sigma));kernel[i+r]=v;total+=v}
        for(i in kernel.indices)kernel[i]/=total
        val tmp=IntArray(w*h);val out=IntArray(w*h)
        for(y in 0 until h)for(x in 0 until w){var a=0f;var rr=0f;var gg=0f;var bb=0f
            for(k in -r..r){val p=src[y*w+(x+k).coerceIn(0,w-1)];val f=kernel[k+r];a+=(p ushr 24)*f;rr+=(p shr 16 and 255)*f;gg+=(p shr 8 and 255)*f;bb+=(p and 255)*f}
            tmp[y*w+x]=(a.toInt().coerceIn(0,255) shl 24) or (rr.toInt().coerceIn(0,255) shl 16) or (gg.toInt().coerceIn(0,255) shl 8) or bb.toInt().coerceIn(0,255)
        }
        for(y in 0 until h)for(x in 0 until w){var a=0f;var rr=0f;var gg=0f;var bb=0f
            for(k in -r..r){val p=tmp[(y+k).coerceIn(0,h-1)*w+x];val f=kernel[k+r];a+=(p ushr 24)*f;rr+=(p shr 16 and 255)*f;gg+=(p shr 8 and 255)*f;bb+=(p and 255)*f}
            out[y*w+x]=(a.toInt().coerceIn(0,255) shl 24) or (rr.toInt().coerceIn(0,255) shl 16) or (gg.toInt().coerceIn(0,255) shl 8) or bb.toInt().coerceIn(0,255)
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(out,0,w,0,0,w,h)}
    }
    fun motionBlur(source:Bitmap,distance:Int,angleDegrees:Float):Bitmap {
        val d=distance.coerceIn(1,32);val w=source.width;val h=source.height;val src=IntArray(w*h);source.getPixels(src,0,w,0,0,w,h);val out=IntArray(w*h)
        val rad=Math.toRadians(angleDegrees.toDouble());val dx=kotlin.math.cos(rad).toFloat();val dy=kotlin.math.sin(rad).toFloat()
        for(y in 0 until h)for(x in 0 until w){var a=0;var r=0;var g=0;var b=0;var n=0
            for(k in -d..d){val sx=(x+(dx*k).toInt()).coerceIn(0,w-1);val sy=(y+(dy*k).toInt()).coerceIn(0,h-1);val p=src[sy*w+sx];a+=p ushr 24;r+=p shr 16 and 255;g+=p shr 8 and 255;b+=p and 255;n++}
            out[y*w+x]=(a/n shl 24) or (r/n shl 16) or (g/n shl 8) or b/n
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(out,0,w,0,0,w,h)}
    }
    fun vignette(source:Bitmap,strength:Float):Bitmap {
        val amount=strength.coerceIn(0f,1f)
        val w=source.width;val h=source.height
        val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h)
        val cx=w/2f;val cy=h/2f;val maxD=sqrt(cx*cx+cy*cy).coerceAtLeast(1f)
        for(y in 0 until h)for(x in 0 until w){
            val i=y*w+x;val p=pixels[i]
            val d=(sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy))/maxD).coerceIn(0f,1f)
            val f=1f-amount*d*d
            val rr=((p shr 16 and 255)*f).toInt().coerceIn(0,255)
            val gg=((p shr 8 and 255)*f).toInt().coerceIn(0,255)
            val bb=((p and 255)*f).toInt().coerceIn(0,255)
            pixels[i]=(p and 0xFF000000) or (rr shl 16) or (gg shl 8) or bb
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
    }
    fun grain(source:Bitmap,intensity:Float,seed:Int=0):Bitmap {
        val amount=intensity.coerceIn(0f,1f)*40f
        val random=Random(seed);val w=source.width;val h=source.height
        val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h)
        for(i in pixels.indices){
            val p=pixels[i];val n=random.nextInt(-amount.toInt(),amount.toInt()+1)
            val rr=((p shr 16 and 255)+n).coerceIn(0,255)
            val gg=((p shr 8 and 255)+n).coerceIn(0,255)
            val bb=((p and 255)+n).coerceIn(0,255)
            pixels[i]=(p and 0xFF000000) or (rr shl 16) or (gg shl 8) or bb
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
    }
}
