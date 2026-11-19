package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

/** Pixel mask helpers; white means selected, black means outside selection. */
object SelectionMaskProcessor {
    fun rectangle(width:Int,height:Int,left:Float,top:Float,right:Float,bottom:Float):Bitmap {
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1)
        val l=(left.coerceIn(0f,1f)*w).toInt().coerceIn(0,w)
        val t=(top.coerceIn(0f,1f)*h).toInt().coerceIn(0,h)
        val r=(right.coerceIn(0f,1f)*w).toInt().coerceIn(l,w)
        val b=(bottom.coerceIn(0f,1f)*h).toInt().coerceIn(t,h)
        val out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        val pixels=IntArray(w*h)
        for(y in t until b) for(x in l until r)pixels[y*w+x]=Color.WHITE
        out.setPixels(pixels,0,w,0,0,w,h)
        return out
    }
    fun ellipse(width:Int,height:Int,left:Float,top:Float,right:Float,bottom:Float):Bitmap {
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1)
        val l=left.coerceIn(0f,1f)*w;val t=top.coerceIn(0f,1f)*h
        val r=right.coerceIn(0f,1f)*w;val b=bottom.coerceIn(0f,1f)*h
        val cx=(l+r)/2f;val cy=(t+b)/2f;val rx=((r-l)/2f).coerceAtLeast(.0001f);val ry=((b-t)/2f).coerceAtLeast(.0001f)
        val pixels=IntArray(w*h)
        for(y in 0 until h)for(x in 0 until w){val dx=(x+.5f-cx)/rx;val dy=(y+.5f-cy)/ry;if(dx*dx+dy*dy<=1f)pixels[y*w+x]=Color.WHITE}
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
    }
    fun invert(mask:Bitmap):Bitmap {
        val pixels=IntArray(mask.width*mask.height);mask.getPixels(pixels,0,mask.width,0,0,mask.width,mask.height)
        for(i in pixels.indices){val v=Color.red(pixels[i]);pixels[i]=Color.argb(Color.alpha(pixels[i]),255-v,255-v,255-v)}
        return Bitmap.createBitmap(mask.width,mask.height,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,mask.width,0,0,mask.width,mask.height)}
    }
    /** Radius-based square morphology: positive expands selection, negative contracts it. */
    fun expandContract(mask:Bitmap,radius:Int):Bitmap {
        if(radius==0)return mask.copy(Bitmap.Config.ARGB_8888,true)
        val w=mask.width;val h=mask.height;val src=IntArray(w*h);mask.getPixels(src,0,w,0,0,w,h)
        val out=IntArray(w*h);val r=radius.coerceIn(-64,64);val dilate=r>0;val size=kotlin.math.abs(r)
        for(y in 0 until h)for(x in 0 until w){var selected=if(dilate)false else true
            loop@for(dy in -size..size)for(dx in -size..size){val nx=x+dx;val ny=y+dy
                val value=if(nx in 0 until w && ny in 0 until h)Color.red(src[ny*w+nx])>=128 else false
                if(dilate&&value){selected=true;break@loop}
                if(!dilate&&!value){selected=false;break@loop}
            }
            out[y*w+x]=if(selected)Color.WHITE else Color.BLACK
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(out,0,w,0,0,w,h)}
    }
    /** Fast box blur feathering with edge clamping; radius is capped for predictable preview cost. */
    fun feather(mask:Bitmap,radius:Int):Bitmap {
        val r=radius.coerceIn(0,32);if(r==0)return mask.copy(Bitmap.Config.ARGB_8888,true)
        val w=mask.width;val h=mask.height;val src=IntArray(w*h);mask.getPixels(src,0,w,0,0,w,h)
        val temp=IntArray(w*h);val out=IntArray(w*h)
        for(y in 0 until h)for(x in 0 until w){var sum=0;var count=0;for(dx in -r..r){sum+=Color.red(src[y*w+(x+dx).coerceIn(0,w-1)]);count++};temp[y*w+x]=sum/count}
        for(y in 0 until h)for(x in 0 until w){var sum=0;var count=0;for(dy in -r..r){sum+=temp[(y+dy).coerceIn(0,h-1)*w+x];count++};val v=sum/count;out[y*w+x]=Color.rgb(v,v,v)}
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(out,0,w,0,0,w,h)}
    }
}
