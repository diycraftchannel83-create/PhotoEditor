package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import com.winsmediatama.winsphotostudio.core.tools.RetouchMode
import com.winsmediatama.winsphotostudio.core.tools.RetouchPoint
import com.winsmediatama.winsphotostudio.core.tools.RetouchStroke
import com.winsmediatama.winsphotostudio.core.tools.RetouchRenderer

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
        value?.takeIf{it!==source}?.recycle();value=null
        value=withContext(Dispatchers.IO){renderRetouch(source,strokes+listOfNotNull(active),strength,offsetX,offsetY)}
    }
    androidx.compose.runtime.DisposableEffect(preview){onDispose{preview?.takeIf{it!==source}?.recycle()}}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp)) {
            Text("Retouch",Modifier.weight(1f),color=Color.White)
            TextButton(onClick={if(strokes.isNotEmpty())strokes=strokes.dropLast(1)}){Text("Undo stroke")}
            TextButton(onClick={strokes=emptyList();active=null},enabled=strokes.isNotEmpty()||active!=null){Text("Clear")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let{onApply(it.copy(Bitmap.Config.ARGB_8888,true))}}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().aspectRatio(source.width.toFloat()/source.height).padding(12.dp).background(Color(0xFF25282D))) {
            preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize())}
            Canvas(Modifier.fillMaxSize().pointerInput(mode,brushSize) {
                detectDragGestures(onDragStart={start->active=RetouchStroke(points=listOf(RetouchPoint(start.x/size.width,start.y/size.height)),size=brushSize,mode=mode)},onDragEnd={active?.let{strokes=strokes+it};active=null},onDragCancel={active=null}) { change,_ ->
                    val next=RetouchPoint(change.position.x/size.width,change.position.y/size.height)
                    val current=active
                    if(current!=null){
                        val previous=current.points.lastOrNull()?:next
                        val dx=(next.x-previous.x)*source.width
                        val dy=(next.y-previous.y)*source.height
                        val distance=kotlin.math.sqrt(dx*dx+dy*dy)
                        val spacing=(current.size*.22f).coerceAtLeast(1f)
                        val steps=(distance/spacing).toInt().coerceAtLeast(1)
                        val interpolated=(1..steps).map{i->RetouchPoint(previous.x+(next.x-previous.x)*i/steps,previous.y+(next.y-previous.y)*i/steps)}
                        active=current.copy(points=current.points+interpolated)
                    }
                    change.consume()
                }
            }) {
                (strokes+listOfNotNull(active)).forEach { s->if(s.points.size==1) drawCircle(Color(0xFF77D7C0),radius=s.size*size.width/source.width/2f,center=Offset(s.points.first().x*size.width,s.points.first().y*size.height)) else if(s.points.size>1) {
                    val path=Path().apply{moveTo(s.points.first().x*size.width,s.points.first().y*size.height);s.points.drop(1).forEach{lineTo(it.x*size.width,it.y*size.height)}}
                    drawPath(path,Color(0xFF77D7C0),style=Stroke(s.size*size.width/source.width))
                }}
        }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)) {
            Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){RetouchMode.entries.forEach{m->FilterChip(selected=mode==m,onClick={mode=m},label={Text(m.label)})}}
            Text("Strokes: ${strokes.size + if(active!=null) 1 else 0}",color=Color.LightGray);Text("Brush size ${brushSize.toInt()} px",color=Color.LightGray);Slider(value=brushSize,onValueChange={brushSize=it},valueRange=5f..180f)
            Text("Strength ${(strength*100).toInt()}%",color=Color.LightGray);Slider(value=strength,onValueChange={strength=it},valueRange=.1f..1f)
            if(mode==RetouchMode.CLONE||mode==RetouchMode.SMUDGE) {
                Text("Source offset X: ${offsetX.toInt()}px",color=Color.LightGray);Slider(value=offsetX,onValueChange={offsetX=it},valueRange=-source.width.toFloat()..source.width.toFloat())
                Text("Source offset Y: ${offsetY.toInt()}px",color=Color.LightGray);Slider(value=offsetY,onValueChange={offsetY=it},valueRange=-source.height.toFloat()..source.height.toFloat())
            }
        }
    }
}

private fun renderRetouch(source:Bitmap,strokes:List<RetouchStroke>,strength:Float,offsetX:Float,offsetY:Float):Bitmap =
    RetouchRenderer.render(source,strokes,strength,offsetX,offsetY)
