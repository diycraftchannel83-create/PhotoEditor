package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import android.os.CancellationSignal
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/** CPU effects used by the replayable effect stack. Supports row-level cancellation for previews. */
object EffectProcessor {
    private const val MAX_PIXELS = 24_000_000L

    private fun read(source: Bitmap): Pair<IntArray, Pair<Int, Int>> {
        val w = source.width; val h = source.height
        require(w > 0 && h > 0 && w.toLong() * h.toLong() <= MAX_PIXELS) { "Effect processing exceeds 24 MP safety limit" }
        return IntArray(w * h).also { source.getPixels(it, 0, w, 0, 0, w, h) } to (w to h)
    }

    private fun create(width: Int, height: Int, pixels: IntArray, cancellation: CancellationSignal? = null): Bitmap {
        cancellation?.throwIfCanceled()
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            output.setPixels(pixels, 0, width, 0, 0, width, height)
            cancellation?.throwIfCanceled()
            return output
        } catch (error: Throwable) {
            if (!output.isRecycled) output.recycle()
            throw error
        }
    }

    fun gaussianBlur(source: Bitmap, radius: Int, cancellation: CancellationSignal? = null): Bitmap {
        val r = radius.coerceIn(0, 24)
        cancellation?.throwIfCanceled()
        if (r == 0) return source.copy(Bitmap.Config.ARGB_8888, true)
        val (src, dimensions) = read(source); val (w, h) = dimensions
        val sigma = max(.5f, r / 2f)
        val kernel = FloatArray(2 * r + 1); var total = 0f
        for (i in -r..r) { val value = exp(-(i * i).toFloat() / (2f * sigma * sigma)); kernel[i + r] = value; total += value }
        for (i in kernel.indices) kernel[i] /= total
        val tmp = IntArray(w * h); val out = IntArray(w * h)
        for (y in 0 until h) {
            cancellation?.throwIfCanceled()
            for (x in 0 until w) {
                var a=0f;var rr=0f;var gg=0f;var bb=0f
                for (k in -r..r) { val p=src[y*w+(x+k).coerceIn(0,w-1)];val f=kernel[k+r];a+=(p ushr 24)*f;rr+=(p shr 16 and 255)*f;gg+=(p shr 8 and 255)*f;bb+=(p and 255)*f }
                tmp[y*w+x]=(a.toInt().coerceIn(0,255) shl 24) or (rr.toInt().coerceIn(0,255) shl 16) or (gg.toInt().coerceIn(0,255) shl 8) or bb.toInt().coerceIn(0,255)
            }
        }
        for (y in 0 until h) {
            cancellation?.throwIfCanceled()
            for (x in 0 until w) {
                var a=0f;var rr=0f;var gg=0f;var bb=0f
                for (k in -r..r) { val p=tmp[(y+k).coerceIn(0,h-1)*w+x];val f=kernel[k+r];a+=(p ushr 24)*f;rr+=(p shr 16 and 255)*f;gg+=(p shr 8 and 255)*f;bb+=(p and 255)*f }
                out[y*w+x]=(a.toInt().coerceIn(0,255) shl 24) or (rr.toInt().coerceIn(0,255) shl 16) or (gg.toInt().coerceIn(0,255) shl 8) or bb.toInt().coerceIn(0,255)
            }
        }
        return create(w,h,out,cancellation)
    }

    fun motionBlur(source: Bitmap, distance: Int, angleDegrees: Float, cancellation: CancellationSignal? = null): Bitmap {
        val d=distance.coerceIn(1,32);val (src,dimensions)=read(source);val (w,h)=dimensions;val out=IntArray(w*h)
        val radians=Math.toRadians(angleDegrees.toDouble());val dx=kotlin.math.cos(radians).toFloat();val dy=kotlin.math.sin(radians).toFloat()
        for(y in 0 until h){cancellation?.throwIfCanceled();for(x in 0 until w){
            var a=0;var r=0;var g=0;var b=0;var n=0
            for(k in -d..d){val sx=(x+(dx*k).toInt()).coerceIn(0,w-1);val sy=(y+(dy*k).toInt()).coerceIn(0,h-1);val p=src[sy*w+sx];a+=p ushr 24;r+=p shr 16 and 255;g+=p shr 8 and 255;b+=p and 255;n++}
            out[y*w+x]=(a/n shl 24)or(r/n shl 16)or(g/n shl 8)or(b/n)
        }}
        return create(w,h,out,cancellation)
    }

    fun vignette(source: Bitmap, strength: Float, cancellation: CancellationSignal? = null): Bitmap {
        val amount=strength.coerceIn(0f,1f);val (pixels,dimensions)=read(source);val (w,h)=dimensions
        val cx=w/2f;val cy=h/2f;val maxDistance=sqrt(cx*cx+cy*cy).coerceAtLeast(1f)
        for(y in 0 until h){cancellation?.throwIfCanceled();for(x in 0 until w){
            val i=y*w+x;val p=pixels[i];val distance=(sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy))/maxDistance).coerceIn(0f,1f);val factor=1f-amount*distance*distance
            val rr=((p shr 16 and 255)*factor).toInt().coerceIn(0,255);val gg=((p shr 8 and 255)*factor).toInt().coerceIn(0,255);val bb=((p and 255)*factor).toInt().coerceIn(0,255)
            pixels[i]=(p and -0x1000000)or(rr shl 16)or(gg shl 8)or bb
        }}
        return create(w,h,pixels,cancellation)
    }

    fun grain(source: Bitmap, intensity: Float, seed: Int = 0, cancellation: CancellationSignal? = null): Bitmap {
        val amount=intensity.coerceIn(0f,1f)*40f;val random=Random(seed);val (pixels,dimensions)=read(source);val (w,h)=dimensions
        for(y in 0 until h){cancellation?.throwIfCanceled();for(x in 0 until w){val i=y*w+x;val p=pixels[i];val noise=random.nextInt(-amount.toInt(),amount.toInt()+1)
            val rr=((p shr 16 and 255)+noise).coerceIn(0,255);val gg=((p shr 8 and 255)+noise).coerceIn(0,255);val bb=((p and 255)+noise).coerceIn(0,255)
            pixels[i]=(p and -0x1000000)or(rr shl 16)or(gg shl 8)or bb
        }}
        return create(w,h,pixels,cancellation)
    }
}
