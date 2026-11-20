package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Bitmap
import android.os.CancellationSignal
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** CPU reference implementations for retouching tools with optional render cancellation. */
object RetouchProcessors {
    fun dodgeBurn(source:Bitmap,amount:Float,exposure:Float,mask:Bitmap?=null,cancellation:CancellationSignal?=null):Bitmap {
        val w=source.width;val h=source.height
        val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h)
        val maskPixels=mask?.let{IntArray(it.width*it.height).also{arr->it.getPixels(arr,0,it.width,0,0,it.width,it.height)}}
        val factor=2f.pow(exposure.coerceIn(-2f,2f))*amount.coerceIn(-1f,1f)
        for(y in 0 until h){cancellation?.throwIfCanceled();for(x in 0 until w){val i=y*w+x;val p=pixels[i];val a=p ushr 24
            val m=maskPixels?.let{arr->val mx=(x.toFloat()/w*mask!!.width).toInt().coerceIn(0,mask.width-1);val my=(y.toFloat()/h*mask.height).toInt().coerceIn(0,mask.height-1);val mp=arr[my*mask.width+mx];val lum=((mp shr 16 and 255)*.2126f+(mp shr 8 and 255)*.7152f+(mp and 255)*.0722f)/255f;(mp ushr 24)/255f*lum}?:1f
            val local=factor*m;val r=adjust(p shr 16 and 255,local);val g=adjust(p shr 8 and 255,local);val b=adjust(p and 255,local)
            pixels[i]=(a shl 24)or(r shl 16)or(g shl 8)or b
        }}
        cancellation?.throwIfCanceled()
        val output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        try{output.setPixels(pixels,0,w,0,0,w,h);cancellation?.throwIfCanceled();return output}catch(error:Throwable){if(!output.isRecycled)output.recycle();throw error}
    }
    /** Spot healing replaces a circular blemish with a local ring average, feathered by distance. */
    fun spotHeal(source:Bitmap,centerX:Int,centerY:Int,radius:Int,cancellation:CancellationSignal?=null):Bitmap {
        val r=radius.coerceIn(1,100);val w=source.width;val h=source.height
        val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h);val original=pixels.copyOf();val ring=max(2,r/2)
        var sr=0L;var sg=0L;var sb=0L;var count=0
        for(y in max(0,centerY-r-ring)..min(h-1,centerY+r+ring)){cancellation?.throwIfCanceled();for(x in max(0,centerX-r-ring)..min(w-1,centerX+r+ring)){
            val d=sqrt(((x-centerX)*(x-centerX)+(y-centerY)*(y-centerY)).toFloat())
            if(d in (r+1f)..(r+ring.toFloat())){val p=original[y*w+x];sr+=(p shr 16 and 255);sg+=(p shr 8 and 255);sb+=(p and 255);count++}
        }}
        if(count==0)return source.copy(Bitmap.Config.ARGB_8888,true)
        val rr=(sr/count).toInt();val gg=(sg/count).toInt();val bb=(sb/count).toInt()
        for(y in max(0,centerY-r)..min(h-1,centerY+r)){cancellation?.throwIfCanceled();for(x in max(0,centerX-r)..min(w-1,centerX+r)){
            val d=sqrt(((x-centerX)*(x-centerX)+(y-centerY)*(y-centerY)).toFloat());if(d>r)continue
            val i=y*w+x;val p=original[i];val blend=(1f-d/r.toFloat()).coerceIn(0f,1f)
            pixels[i]=(p and -0x1000000)or(lerp(p shr 16 and 255,rr,blend) shl 16)or(lerp(p shr 8 and 255,gg,blend) shl 8)or lerp(p and 255,bb,blend)
        }}
        cancellation?.throwIfCanceled()
        val output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        try{output.setPixels(pixels,0,w,0,0,w,h);cancellation?.throwIfCanceled();return output}catch(error:Throwable){if(!output.isRecycled)output.recycle();throw error}
    }
    /** Smudge samples pixels from the source at a translated location and mixes by strength. */
    fun smudge(source:Bitmap,offsetX:Int,offsetY:Int,strength:Float,cancellation:CancellationSignal?=null):Bitmap {
        val w=source.width;val h=source.height;val src=IntArray(w*h);source.getPixels(src,0,w,0,0,w,h);val out=src.copyOf();val mix=strength.coerceIn(0f,1f)
        for(y in 0 until h){cancellation?.throwIfCanceled();for(x in 0 until w){val sx=(x+offsetX).coerceIn(0,w-1);val sy=(y+offsetY).coerceIn(0,h-1);val i=y*w+x;val p=src[i];val q=src[sy*w+sx]
            out[i]=(p and -0x1000000)or(lerp(p shr 16 and 255,q shr 16 and 255,mix) shl 16)or(lerp(p shr 8 and 255,q shr 8 and 255,mix) shl 8)or lerp(p and 255,q and 255,mix)
        }}
        cancellation?.throwIfCanceled()
        val output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        try{output.setPixels(out,0,w,0,0,w,h);cancellation?.throwIfCanceled();return output}catch(error:Throwable){if(!output.isRecycled)output.recycle();throw error}
    }
    private fun adjust(v:Int,amount:Float)=if(amount>=0)(v+(255-v)*amount).toInt().coerceIn(0,255) else (v*(1f+amount)).toInt().coerceIn(0,255)
    private fun lerp(a:Int,b:Int,t:Float)=(a+(b-a)*t).toInt().coerceIn(0,255)
    private fun Float.pow(exponent:Float)=kotlin.math.exp(kotlin.math.ln(this)*exponent)
}
