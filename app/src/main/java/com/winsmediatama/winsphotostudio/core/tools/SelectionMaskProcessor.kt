package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

/** Pixel mask helpers; white means selected, black means outside selection. */
object SelectionMaskProcessor {
    fun polygon(width:Int,height:Int,points:List<Pair<Float,Float>>):Bitmap {
        require(points.size>=3){"Polygon needs at least three points"}
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1);val pixels=IntArray(w*h)
        for(y in 0 until h)for(x in 0 until w){
            val px=(x+.5f)/w;val py=(y+.5f)/h;var inside=false
            var j=points.lastIndex
            for(i in points.indices){val xi=points[i].first;val yi=points[i].second;val xj=points[j].first;val yj=points[j].second
                if(((yi>py)!=(yj>py)) && (px<(xj-xi)*(py-yi)/(yj-yi+0.000001f)+xi))inside=!inside
                j=i
            }
            if(inside)pixels[y*w+x]=Color.WHITE
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
    }
    /** RGB distance selection, normalized to 0..1; optional softness applies a linear feather band. */
    fun colorRange(source:Bitmap,sampleColor:Int,tolerance:Float,softness:Float=0f):Bitmap {
        val tol=tolerance.coerceIn(0f,1f)*441.67295f;val soft=softness.coerceIn(0f,1f)*220.83648f
        val sr=android.graphics.Color.red(sampleColor);val sg=android.graphics.Color.green(sampleColor);val sb=android.graphics.Color.blue(sampleColor)
        val w=source.width;val h=source.height;val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h)
        for(i in pixels.indices){val p=pixels[i];val dr=android.graphics.Color.red(p)-sr;val dg=android.graphics.Color.green(p)-sg;val db=android.graphics.Color.blue(p)-sb
            val d=kotlin.math.sqrt((dr*dr+dg*dg+db*db).toFloat())
            val value=if(soft<=0f)if(d<=tol)255 else 0 else ((tol+soft-d)/soft*255f).toInt().coerceIn(0,255)
            pixels[i]=Color.argb(255,value,value,value)
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
    }
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
