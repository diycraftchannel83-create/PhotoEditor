package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Bitmap

fun RetouchRenderer.render(source:Bitmap,strokes:List<RetouchStroke>,strength:Float,offsetX:Float,offsetY:Float):Bitmap {
    if(strokes.isEmpty())return source
    val w=source.width;val h=source.height
    val base=IntArray(w*h);source.getPixels(base,0,w,0,0,w,h)
    var result=base.copyOf()
    val blur=if(strokes.any{it.mode==RetouchMode.BLUR})boxBlur(base,w,h,3)else base
    val pixel=if(strokes.any{it.mode==RetouchMode.PIXELATE})pixelate(base,w,h,12)else base
    for(stroke in strokes){
        val mode=stroke.mode
        val group=listOf(stroke)
        val mask=FloatArray(w*h)
        group.forEach{stroke->stroke.points.forEach{point->
            val cx=(point.x*w).toInt();val cy=(point.y*h).toInt();val radius=(stroke.size/2f).toInt().coerceAtLeast(1)
            for(y in (cy-radius).coerceAtLeast(0)..(cy+radius).coerceAtMost(h-1))for(x in (cx-radius).coerceAtLeast(0)..(cx+radius).coerceAtMost(w-1)){
                val d=kotlin.math.sqrt(((x-cx)*(x-cx)+(y-cy)*(y-cy)).toFloat());if(d<=radius){val edge=(1f-d/radius).coerceIn(0f,1f);val smooth=edge*edge*(3f-2f*edge);mask[y*w+x]=maxOf(mask[y*w+x],smooth)}
            }
        }}
        when(mode){
            RetouchMode.SPOT_HEAL->group.forEach{stroke->stroke.points.forEach{point->
                val cx=(point.x*w).toInt();val cy=(point.y*h).toInt();val radius=(stroke.size/2f).toInt().coerceAtLeast(1);val ring=(radius/2).coerceAtLeast(2)
                var rr=0L;var gg=0L;var bb=0L;var count=0
                for(y in (cy-radius-ring).coerceAtLeast(0)..(cy+radius+ring).coerceAtMost(h-1))for(x in (cx-radius-ring).coerceAtLeast(0)..(cx+radius+ring).coerceAtMost(w-1)){
                    val d=kotlin.math.sqrt(((x-cx)*(x-cx)+(y-cy)*(y-cy)).toFloat())
                    if(d>radius&&d<=radius+ring){val q=result[y*w+x];rr+=(q shr 16)and 255;gg+=(q shr 8)and 255;bb+=q and 255;count++}
                }
                if(count>0){val healing=(rr/count shl 16)or(gg/count shl 8)or(bb/count)
                    for(y in (cy-radius).coerceAtLeast(0)..(cy+radius).coerceAtMost(h-1))for(x in (cx-radius).coerceAtLeast(0)..(cx+radius).coerceAtMost(w-1)){
                        val i=y*w+x;val d=kotlin.math.sqrt(((x-cx)*(x-cx)+(y-cy)*(y-cy)).toFloat())
                        if(d<=radius){val t=(strength*(1f-d/radius).coerceIn(0f,1f));result[i]=blend(result[i],(result[i] and 0xFF000000)or healing,t)}
                    }
                }
            }}
            RetouchMode.DODGE,RetouchMode.BURN->{
                val maskBitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);val mp=IntArray(w*h){i->val v=(mask[i]*255).toInt();android.graphics.Color.argb(255,v,v,v)};maskBitmap.setPixels(mp,0,w,0,0,w,h)
                val working=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(result,0,w,0,0,w,h)}
                val adjusted=com.winsmediatama.winsphotostudio.core.tools.RetouchProcessors.dodgeBurn(working,if(mode==RetouchMode.DODGE).5f else -.5f,.5f,maskBitmap)
                if(adjusted!==working)working.recycle()
                val ap=IntArray(w*h);adjusted.getPixels(ap,0,w,0,0,w,h);if(!adjusted.isRecycled)adjusted.recycle();maskBitmap.recycle()
                for(i in result.indices)result[i]=blend(result[i],ap[i],strength)
            }
            RetouchMode.SMUDGE->{
                val working=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(result,0,w,0,0,w,h)}
                val shifted=com.winsmediatama.winsphotostudio.core.tools.RetouchProcessors.smudge(working,offsetX.toInt(),offsetY.toInt(),strength)
                if(shifted!==working)working.recycle()
                val sp=IntArray(w*h);shifted.getPixels(sp,0,w,0,0,w,h);if(!shifted.isRecycled)shifted.recycle()
                for(i in result.indices)result[i]=blend(result[i],sp[i],mask[i]*strength)
            }
            else->for(i in result.indices){val m=mask[i]*strength;if(m<=0f)continue;val target=when(mode){RetouchMode.BLUR->blur[i];RetouchMode.PIXELATE->pixel[i];RetouchMode.CLONE->{val x=i%w;val y=i/w;val sx=(x-offsetX.toInt()).coerceIn(0,w-1);val sy=(y-offsetY.toInt()).coerceIn(0,h-1);base[sy*w+sx]};else->result[i]};result[i]=blend(result[i],target,m)}
        }
    }
    return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also{it.setPixels(result,0,w,0,0,w,h)}
}
private fun blend(a:Int,b:Int,t:Float):Int {
    fun ch(shift:Int)=(((a shr shift)and 255)*(1-t)+((b shr shift)and 255)*t).toInt().coerceIn(0,255)
    return (a and -0x1000000) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}
private fun boxBlur(p:IntArray,w:Int,h:Int,r:Int):IntArray {
    val radius=r.coerceIn(1,16);val temp=IntArray(p.size);val out=IntArray(p.size)
    for(y in 0 until h)for(x in 0 until w){var rs=0;var gs=0;var bs=0;var n=0
        for(dx in -radius..radius){val q=p[y*w+(x+dx).coerceIn(0,w-1)];rs+=(q shr 16)and 255;gs+=(q shr 8)and 255;bs+=q and 255;n++}
        temp[y*w+x]=(p[y*w+x] and -0x1000000)or((rs/n) shl 16)or((gs/n) shl 8)or(bs/n)
    }
    for(y in 0 until h)for(x in 0 until w){var rs=0;var gs=0;var bs=0;var n=0
        for(dy in -radius..radius){val q=temp[(y+dy).coerceIn(0,h-1)*w+x];rs+=(q shr 16)and 255;gs+=(q shr 8)and 255;bs+=q and 255;n++}
        out[y*w+x]=(p[y*w+x] and -0x1000000)or((rs/n) shl 16)or((gs/n) shl 8)or(bs/n)
    }
    return out
}
private fun pixelate(p:IntArray,w:Int,h:Int,block:Int):IntArray {
    val out=p.copyOf()
    for(by in 0 until h step block)for(bx in 0 until w step block) {
        var r=0;var g=0;var b=0;var n=0
        for(y in by until (by+block).coerceAtMost(h))for(x in bx until (bx+block).coerceAtMost(w)){val q=p[y*w+x];r+=(q shr 16)and 255;g+=(q shr 8)and 255;b+=q and 255;n++}
        val avg=(r/n shl 16) or (g/n shl 8) or (b/n)
        for(y in by until (by+block).coerceAtMost(h))for(x in bx until (bx+block).coerceAtMost(w))out[y*w+x]=(p[y*w+x] and -0x1000000) or avg
    }
    return out
}
