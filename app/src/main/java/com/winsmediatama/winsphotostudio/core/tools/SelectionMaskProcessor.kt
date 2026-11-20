package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

/** Pixel mask helpers; white means selected, black means outside selection. */
object SelectionMaskProcessor {
    private const val MAX_PIXELS=24_000_000L
    private fun checkSize(width:Int,height:Int) {
        require(width>0&&height>0&&width.toLong()*height.toLong()<=MAX_PIXELS) { "Selection mask exceeds 24 MP safety limit" }
    }
    fun magicWand(source:Bitmap,seedX:Int,seedY:Int,tolerance:Float):Bitmap {
        val w=source.width;val h=source.height;checkSize(w,h)
        val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h)
        val seed=seedY.coerceIn(0,h-1)*w+seedX.coerceIn(0,w-1)
        val target=pixels[seed]
        val tr=Color.red(target);val tg=Color.green(target);val tb=Color.blue(target)
        val threshold=tolerance.coerceIn(0f,1f)*510f
        val selected=BooleanArray(w*h);val queue=java.util.ArrayDeque<Int>()
        queue.add(seed);selected[seed]=true
        while(queue.isNotEmpty()){
            val index=queue.removeFirst();val p=pixels[index]
            val dr=Color.red(p)-tr;val dg=Color.green(p)-tg;val db=Color.blue(p)-tb
            if(sqrt((dr*dr+dg*dg+db*db).toFloat())>threshold)continue
            val x=index%w;val y=index/w
            val neighbors=intArrayOf(
                if(x>0)index-1 else -1,
                if(x<w-1)index+1 else -1,
                if(y>0)index-w else -1,
                if(y<h-1)index+w else -1
            )
            for(neighbor in neighbors)if(neighbor>=0&&!selected[neighbor]){selected[neighbor]=true;queue.add(neighbor)}
        }
        val out=IntArray(w*h){i->if(selected[i]&&distance(pixels[i],target)<=threshold&&Color.alpha(pixels[i])>0)Color.WHITE else Color.BLACK}
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(out,0,w,0,0,w,h)}
    }
    private fun distance(a:Int,b:Int):Float {
        val dr=Color.red(a)-Color.red(b);val dg=Color.green(a)-Color.green(b)
        val db=Color.blue(a)-Color.blue(b);val da=Color.alpha(a)-Color.alpha(b)
        return sqrt((dr*dr+dg*dg+db*db+da*da).toFloat())
    }
    fun polygon(width:Int,height:Int,points:List<Pair<Float,Float>>):Bitmap {
        checkSize(width.coerceAtLeast(1),height.coerceAtLeast(1))
        require(points.size>=3){"Polygon needs at least three points"}
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1);val pixels=IntArray(w*h)
        for(y in 0 until h)for(x in 0 until w){
            val px=(x+.5f)/w;val py=(y+.5f)/h;var inside=false
            var j=points.lastIndex
            for(i in points.indices){
                val xi=points[i].first;val yi=points[i].second;val xj=points[j].first;val yj=points[j].second
                if(((yi>py)!=(yj>py))&&(px<(xj-xi)*(py-yi)/(yj-yi+0.000001f)+xi))inside=!inside
                j=i
            }
            if(inside)pixels[y*w+x]=Color.WHITE
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
    }
    fun colorRange(source:Bitmap,sampleColor:Int,tolerance:Float,softness:Float=0f):Bitmap {
        val w=source.width;val h=source.height;checkSize(w,h)
        val tol=tolerance.coerceIn(0f,1f)*441.67295f;val soft=softness.coerceIn(0f,1f)*220.83648f
        val sr=Color.red(sampleColor);val sg=Color.green(sampleColor);val sb=Color.blue(sampleColor)
        val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h)
        for(i in pixels.indices){
            val p=pixels[i];val dr=Color.red(p)-sr;val dg=Color.green(p)-sg;val db=Color.blue(p)-sb
            val d=sqrt((dr*dr+dg*dg+db*db).toFloat())
            val value=if(soft<=0f)if(d<=tol)255 else 0 else ((tol+soft-d)/soft*255f).toInt().coerceIn(0,255)
            pixels[i]=Color.argb(Color.alpha(p),value,value,value)
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
    }
    fun rectangle(width:Int,height:Int,left:Float,top:Float,right:Float,bottom:Float):Bitmap {
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1);checkSize(w,h)
        val l=(left.coerceIn(0f,1f)*w).toInt().coerceIn(0,w)
        val t=(top.coerceIn(0f,1f)*h).toInt().coerceIn(0,h)
        val r=(right.coerceIn(0f,1f)*w).toInt().coerceIn(l,w)
        val b=(bottom.coerceIn(0f,1f)*h).toInt().coerceIn(t,h)
        val pixels=IntArray(w*h)
        for(y in t until b)for(x in l until r)pixels[y*w+x]=Color.WHITE
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
    }
    fun ellipse(width:Int,height:Int,left:Float,top:Float,right:Float,bottom:Float):Bitmap {
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1);checkSize(w,h)
        val l=left.coerceIn(0f,1f)*w;val t=top.coerceIn(0f,1f)*h
        val r=right.coerceIn(0f,1f)*w;val b=bottom.coerceIn(0f,1f)*h
        val cx=(l+r)/2f;val cy=(t+b)/2f;val rx=((r-l)/2f).coerceAtLeast(.0001f);val ry=((b-t)/2f).coerceAtLeast(.0001f)
        val pixels=IntArray(w*h)
        for(y in 0 until h)for(x in 0 until w){val dx=(x+.5f-cx)/rx;val dy=(y+.5f-cy)/ry;if(dx*dx+dy*dy<=1f)pixels[y*w+x]=Color.WHITE}
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
    }
    fun invert(mask:Bitmap):Bitmap {
        checkSize(mask.width,mask.height)
        val pixels=IntArray(mask.width*mask.height);mask.getPixels(pixels,0,mask.width,0,0,mask.width,mask.height)
        for(i in pixels.indices){val v=Color.red(pixels[i]);pixels[i]=Color.argb(Color.alpha(pixels[i]),255-v,255-v,255-v)}
        return Bitmap.createBitmap(mask.width,mask.height,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,mask.width,0,0,mask.width,mask.height)}
    }
    fun expandContract(mask:Bitmap,radius:Int):Bitmap {
        checkSize(mask.width,mask.height)
        if(radius==0)return mask.copy(Bitmap.Config.ARGB_8888,true)
        val w=mask.width;val h=mask.height;val src=IntArray(w*h);mask.getPixels(src,0,w,0,0,w,h)
        val r=radius.coerceIn(-64,64);val dilate=r>0;val size=kotlin.math.abs(r)
        val horizontal=IntArray(w*h);val out=IntArray(w*h)
        for(y in 0 until h)for(x in 0 until w){
            var value=if(dilate)0 else 255
            for(dx in -size..size){val nx=x+dx;val selected=nx in 0 until w && Color.red(src[y*w+nx])>=128
                if(dilate&&selected){value=255;break};if(!dilate&&!selected){value=0;break}}
            horizontal[y*w+x]=value
        }
        for(y in 0 until h)for(x in 0 until w){
            var value=if(dilate)0 else 255
            for(dy in -size..size){val ny=y+dy;val selected=ny in 0 until h && horizontal[ny*w+x]>=128
                if(dilate&&selected){value=255;break};if(!dilate&&!selected){value=0;break}}
            out[y*w+x]=Color.rgb(value,value,value)
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(out,0,w,0,0,w,h)}
    }
    fun feather(mask:Bitmap,radius:Int):Bitmap {
        checkSize(mask.width,mask.height)
        val r=radius.coerceIn(0,32);if(r==0)return mask.copy(Bitmap.Config.ARGB_8888,true)
        val w=mask.width;val h=mask.height;val src=IntArray(w*h);mask.getPixels(src,0,w,0,0,w,h)
        val temp=IntArray(w*h);val out=IntArray(w*h);val window=2*r+1
        for(y in 0 until h){
            var sum=0
            for(dx in -r..r)sum+=Color.red(src[y*w+dx.coerceIn(0,w-1)])
            for(x in 0 until w){
                temp[y*w+x]=sum/window
                sum-=Color.red(src[y*w+(x-r).coerceIn(0,w-1)])
                sum+=Color.red(src[y*w+(x+r+1).coerceIn(0,w-1)])
            }
        }
        for(x in 0 until w){
            var sum=0
            for(dy in -r..r)sum+=temp[dy.coerceIn(0,h-1)*w+x]
            for(y in 0 until h){
                val v=sum/window;out[y*w+x]=Color.rgb(v,v,v)
                sum-=temp[(y-r).coerceIn(0,h-1)*w+x]
                sum+=temp[(y+r+1).coerceIn(0,h-1)*w+x]
            }
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(out,0,w,0,0,w,h)}
    }
}
