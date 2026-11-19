package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class RetouchMode(val label:String){BLUR("Blur"),PIXELATE("Pixelate"),CLONE("Clone"),SPOT_HEAL("Spot heal"),DODGE("Dodge"),BURN("Burn"),SMUDGE("Smudge")}
private data class RetouchStroke(val points:List<Offset>,val size:Float,val mode:RetouchMode)

@Composable
fun RetouchScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var mode by remember { mutableStateOf(RetouchMode.BLUR) }
    var strokes by remember { mutableStateOf<List<RetouchStroke>>(emptyList()) }
    var active by remember { mutableStateOf<RetouchStroke?>(null) }
    var brushSize by remember { mutableFloatStateOf(40f) }
    var strength by remember { mutableFloatStateOf(.6f) }
    var offsetX by remember { mutableFloatStateOf(100f) }
    var offsetY by remember { mutableFloatStateOf(100f) }
    val preview by produceState<Bitmap?>(null,source,strokes,active,mode,brushSize,strength,offsetX,offsetY) {
        value=null
        value=withContext(Dispatchers.IO){renderRetouch(source,strokes+listOfNotNull(active),strength,offsetX,offsetY)}
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp)) {
            Text("Retouch",Modifier.weight(1f),color=Color.White)
            TextButton(onClick={if(strokes.isNotEmpty())strokes=strokes.dropLast(1)}){Text("Undo")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let(onApply)}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().aspectRatio(source.width.toFloat()/source.height).padding(12.dp).background(Color(0xFF25282D))) {
            preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize())}
            Canvas(Modifier.fillMaxSize().pointerInput(mode,brushSize) {
                detectDragGestures(onDragStart={start->active=RetouchStroke(listOf(Offset(start.x/size.width,start.y/size.height)),brushSize,mode)},onDragEnd={active?.let{strokes=strokes+it};active=null},onDragCancel={active=null}) { change,_ ->
                    active=active?.copy(points=active!!.points+Offset(change.position.x/size.width,change.position.y/size.height));change.consume()
                }
            }) {
                (strokes+listOfNotNull(active)).forEach { s->if(s.points.size==1) drawCircle(Color(0xFF77D7C0),radius=s.size*size.width/source.width/2f,center=Offset(s.points.first().x*size.width,s.points.first().y*size.height)) else if(s.points.size>1) {
                    val path=Path().apply{moveTo(s.points.first().x*size.width,s.points.first().y*size.height);s.points.drop(1).forEach{lineTo(it.x*size.width,it.y*size.height)}}
                    drawPath(path,Color(0xFF77D7C0),style=Stroke(s.size*size.width/source.width))
                }}
        }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){RetouchMode.entries.forEach{m->FilterChip(selected=mode==m,onClick={mode=m},label={Text(m.label)})}}
            Text("Brush size ${brushSize.toInt()} px",color=Color.LightGray);Slider(value=brushSize,onValueChange={brushSize=it},valueRange=5f..180f)
            if(mode!=RetouchMode.CLONE){Text("Strength ${(strength*100).toInt()}%",color=Color.LightGray);Slider(value=strength,onValueChange={strength=it},valueRange=.1f..1f)}
            if(mode==RetouchMode.CLONE||mode==RetouchMode.SMUDGE) {
                Text("Source offset X: ${offsetX.toInt()}px",color=Color.LightGray);Slider(value=offsetX,onValueChange={offsetX=it},valueRange=-source.width.toFloat()..source.width.toFloat())
                Text("Source offset Y: ${offsetY.toInt()}px",color=Color.LightGray);Slider(value=offsetY,onValueChange={offsetY=it},valueRange=-source.height.toFloat()..source.height.toFloat())
            }
        }
    }
}

private fun renderRetouch(source:Bitmap,strokes:List<RetouchStroke>,strength:Float,offsetX:Float,offsetY:Float):Bitmap {
    if(strokes.isEmpty())return source
    val w=source.width;val h=source.height
    val base=IntArray(w*h);source.getPixels(base,0,w,0,0,w,h)
    var result=base.copyOf()
    val blur=boxBlur(base,w,h,3)
    val pixel=if(strokes.any{it.mode==RetouchMode.PIXELATE})pixelate(base,w,h,12)else base
    for(mode in RetouchMode.entries){
        val group=strokes.filter{it.mode==mode}
        if(group.isEmpty())continue
        val mask=FloatArray(w*h)
        group.forEach{stroke->stroke.points.forEach{point->
            val cx=(point.x*w).toInt();val cy=(point.y*h).toInt();val radius=(stroke.size/2f).toInt().coerceAtLeast(1)
            for(y in (cy-radius).coerceAtLeast(0)..(cy+radius).coerceAtMost(h-1))for(x in (cx-radius).coerceAtLeast(0)..(cx+radius).coerceAtMost(w-1)){
                val d=(x-cx)*(x-cx)+(y-cy)*(y-cy);if(d<=radius*radius)mask[y*w+x]=1f
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
                val adjusted=com.winsmediatama.winsphotostudio.core.tools.RetouchProcessors.dodgeBurn(Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(result,0,w,0,0,w,h)},if(mode==RetouchMode.DODGE).5f else -.5f,.5f,maskBitmap)
                val ap=IntArray(w*h);adjusted.getPixels(ap,0,w,0,0,w,h);adjusted.recycle();maskBitmap.recycle()
                for(i in result.indices)result[i]=blend(result[i],ap[i],strength)
            }
            RetouchMode.SMUDGE->{
                val shifted=com.winsmediatama.winsphotostudio.core.tools.RetouchProcessors.smudge(Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(result,0,w,0,0,w,h)},offsetX.toInt(),offsetY.toInt(),strength)
                val sp=IntArray(w*h);shifted.getPixels(sp,0,w,0,0,w,h);shifted.recycle()
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
