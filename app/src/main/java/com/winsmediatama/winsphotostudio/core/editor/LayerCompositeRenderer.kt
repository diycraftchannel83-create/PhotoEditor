package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.os.Build
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.CancellationSignal

object LayerCompositeRenderer {
    fun render(layers:List<EditorLayer>,width:Int,height:Int,cancellation:CancellationSignal?=null):Bitmap {
        val w=width.coerceAtLeast(1);val h=height.coerceAtLeast(1)
        require(w.toLong()*h.toLong()<=24_000_000L){"Composite exceeds 24 MP safety limit (${w}x${h})"}
        cancellation?.throwIfCanceled()
        val output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        val owned=java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Bitmap,Boolean>())
        fun own(bitmap:Bitmap):Bitmap { if(bitmap!==output)owned.add(bitmap);return bitmap }
        val canvas=Canvas(output);val dest=Rect(0,0,w,h)
        var index=0
        var previousAlpha:Bitmap?=null
        var previousOwned=false
        try {
        while(index<layers.size){
            cancellation?.throwIfCanceled()
            val layer=layers[index]
            if(layer.groupId.isNullOrBlank()){
                if(layer.visible&&!layer.bitmap.isRecycled){
                    val replayable=LayerOperationRenderer.isReplayable(layer)
                    val replayBitmap=if(replayable)own(LayerOperationRenderer.render(layer))else layer.bitmap
                    val masked=layer.mask?.takeUnless{it.isRecycled}?.let{own(LayerMaskProcessor.apply(replayBitmap,it))}
                    val base=masked?:replayBitmap
                    val clipped=if(layer.clipToBelow&&previousAlpha!=null)own(ClipAlphaProcessor.apply(base,previousAlpha!!))else base
                    drawPrepared(canvas,output,clipped,dest,layer.opacity,layer.blendMode,cancellation)
                    val alphaSource=if(layer.opacity<1f)own(applyAlphaOpacity(clipped,layer.opacity,cancellation))else clipped
                    val nextOwned=alphaSource!==layer.bitmap
                    if(previousOwned&&previousAlpha!==alphaSource)previousAlpha?.takeUnless{it.isRecycled}?.recycle()
                    previousAlpha=alphaSource;previousOwned=nextOwned
                    if(masked!=null&&masked!==clipped&&masked!==replayBitmap&&!masked.isRecycled)masked.recycle()
                    if(alphaSource!==clipped&&clipped!==layer.bitmap&&!clipped.isRecycled)clipped.recycle()
                    if(replayable&&replayBitmap!==alphaSource&&replayBitmap!==clipped&&!replayBitmap.isRecycled)replayBitmap.recycle()
                }
                index++
                continue
            }
            val groupId=layer.groupId
            val group=mutableListOf<EditorLayer>()
            while(index<layers.size&&layers[index].groupId==groupId){group+=layers[index];index++}
            val groupBitmap=own(Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888))
            val groupCanvas=Canvas(groupBitmap)
            var groupPrevious:Bitmap?=null;var groupPreviousOwned=false
            group.filter{it.visible&&!it.bitmap.isRecycled}.forEach{child->
                cancellation?.throwIfCanceled()
                val replayable=LayerOperationRenderer.isReplayable(child)
                val replayBitmap=if(replayable)own(LayerOperationRenderer.render(child))else child.bitmap
                val masked=child.mask?.takeUnless{it.isRecycled}?.let{own(LayerMaskProcessor.apply(replayBitmap,it))}
                val base=masked?:replayBitmap
                val clipped=if(child.clipToBelow&&groupPrevious!=null)own(ClipAlphaProcessor.apply(base,groupPrevious!!))else base
                drawPrepared(groupCanvas,groupBitmap,clipped,dest,child.opacity,child.blendMode,cancellation)
                val alphaSource=if(child.opacity<1f)own(applyAlphaOpacity(clipped,child.opacity,cancellation))else clipped
                val nextOwned=alphaSource!==child.bitmap
                if(groupPreviousOwned&&groupPrevious!==alphaSource)groupPrevious?.takeUnless{it.isRecycled}?.recycle()
                groupPrevious=alphaSource;groupPreviousOwned=nextOwned
                if(masked!=null&&masked!==clipped&&masked!==replayBitmap&&!masked.isRecycled)masked.recycle()
                if(alphaSource!==clipped&&clipped!==child.bitmap&&!clipped.isRecycled)clipped.recycle()
                if(replayable&&replayBitmap!==alphaSource&&replayBitmap!==clipped&&!replayBitmap.isRecycled)replayBitmap.recycle()
            }
            if(groupPreviousOwned)groupPrevious?.takeUnless{it.isRecycled}?.recycle()
            val groupOpacity=(group.firstOrNull()?.groupOpacity?:1f).coerceIn(0f,1f)
            val clippedGroup=if(layer.clipToBelow&&previousAlpha!=null)own(ClipAlphaProcessor.apply(groupBitmap,previousAlpha!!))else groupBitmap
            val groupAlphaSource=if(groupOpacity<1f)own(applyAlphaOpacity(clippedGroup,groupOpacity,cancellation))else clippedGroup
            drawPrepared(canvas,output,groupAlphaSource,dest,1f,layer.groupBlendMode?:EditorBlendMode.NORMAL,cancellation)
            val nextOwned=true
            if(previousOwned&&previousAlpha!==groupAlphaSource)previousAlpha?.takeUnless{it.isRecycled}?.recycle()
            previousAlpha=groupAlphaSource;previousOwned=nextOwned
            if(groupAlphaSource!==clippedGroup&&!groupAlphaSource.isRecycled)clippedGroup.recycle()
            if(clippedGroup!==groupBitmap&&!groupBitmap.isRecycled)groupBitmap.recycle()
        }
        if(previousOwned)previousAlpha?.takeUnless{it.isRecycled}?.recycle()
        return output
        } catch(error:Throwable) {
            if(!output.isRecycled)output.recycle()
            throw error
        } finally {
            owned.forEach { bitmap -> if(!bitmap.isRecycled)bitmap.recycle() }
            owned.clear()
        }
    }
    private fun applyAlphaOpacity(source:Bitmap,opacity:Float,cancellation:CancellationSignal?):Bitmap {
        val w=source.width;val h=source.height
        val output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        try {
            val rowsPerStrip=128
            for(top in 0 until h step rowsPerStrip) {
                cancellation?.throwIfCanceled()
                val rows=minOf(rowsPerStrip,h-top)
                val pixels=IntArray(w*rows)
                source.getPixels(pixels,0,w,0,top,w,rows)
                for(i in pixels.indices) {
                    if(i and 0x3FFF==0)cancellation?.throwIfCanceled()
                    val alpha=((pixels[i] ushr 24)*opacity).toInt().coerceIn(0,255)
                    pixels[i]=(alpha shl 24)or(pixels[i] and 0x00FFFFFF)
                }
                output.setPixels(pixels,0,w,0,top,w,rows)
            }
            return output
        } catch(error:Throwable) { if(!output.isRecycled)output.recycle();throw error }
    }
    private fun drawPrepared(canvas:Canvas,target:Bitmap,source:Bitmap,dest:Rect,opacity:Float,blendMode:EditorBlendMode,cancellation:CancellationSignal?) {
        cancellation?.throwIfCanceled()
        val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply{alpha=(opacity.coerceIn(0f,1f)*255).toInt()}
        if(blendMode==EditorBlendMode.DIFFERENCE&&Build.VERSION.SDK_INT<29){drawDifference(target,source,dest,paint.alpha,cancellation);return}
        if(blendMode==EditorBlendMode.DIFFERENCE&&Build.VERSION.SDK_INT>=29){paint.blendMode=BlendMode.DIFFERENCE}
        else paint.xfermode=PorterDuffXfermode(when(blendMode){
            EditorBlendMode.NORMAL->PorterDuff.Mode.SRC_OVER
            EditorBlendMode.MULTIPLY->PorterDuff.Mode.MULTIPLY
            EditorBlendMode.SCREEN->PorterDuff.Mode.SCREEN
            EditorBlendMode.OVERLAY->PorterDuff.Mode.OVERLAY
            EditorBlendMode.DARKEN->PorterDuff.Mode.DARKEN
            EditorBlendMode.LIGHTEN->PorterDuff.Mode.LIGHTEN
            EditorBlendMode.ADD->PorterDuff.Mode.ADD
            EditorBlendMode.DIFFERENCE->PorterDuff.Mode.XOR
        })
        canvas.drawBitmap(source,null,dest,paint)
    }
    private fun drawDifference(target:Bitmap,source:Bitmap,dest:Rect,opacity:Int,cancellation:CancellationSignal?) {
        cancellation?.throwIfCanceled()
        val w=target.width;val h=target.height
        val base=IntArray(w*h);target.getPixels(base,0,w,0,0,w,h)
        val scaled=Bitmap.createScaledBitmap(source,w,h,true)
        val pixels=IntArray(w*h)
        try { scaled.getPixels(pixels,0,w,0,0,w,h) }
        finally { if(scaled!==source&&!scaled.isRecycled)scaled.recycle() }
        for(i in base.indices){
            if(i and 0x3FFF==0)cancellation?.throwIfCanceled()
            val d=base[i];val s=pixels[i];val da=d ushr 24;val sa=((s ushr 24)*opacity/255);val outA=sa+da*(255-sa)/255
            if(outA<=0){base[i]=0;continue}
            val dr=(d shr 16)and 255;val dg=(d shr 8)and 255;val db=d and 255
            val sr=(s shr 16)and 255;val sg=(s shr 8)and 255;val sb=s and 255
            val r=(dr*(255-sa)+kotlin.math.abs(dr-sr)*sa)/255
            val g=(dg*(255-sa)+kotlin.math.abs(dg-sg)*sa)/255
            val b=(db*(255-sa)+kotlin.math.abs(db-sb)*sa)/255
            base[i]=(outA shl 24)or(r shl 16)or(g shl 8)or b
        }
        target.setPixels(base,0,w,0,0,w,h)
    }
}
