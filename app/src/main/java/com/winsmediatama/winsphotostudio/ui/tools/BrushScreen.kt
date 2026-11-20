package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private data class BrushStroke(val points:List<Offset>,val color:Color,val size:Float,val erase:Boolean,val opacity:Float,val hardness:Float)

@Composable
fun BrushScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var strokes by remember { mutableStateOf<List<BrushStroke>>(emptyList()) }
    var active by remember { mutableStateOf<BrushStroke?>(null) }
    var color by remember { mutableStateOf(Color(0xFFFF3B30)) }
    var erase by remember { mutableStateOf(false) }
    var brushSize by remember { mutableFloatStateOf(24f) }
    var opacity by remember{mutableFloatStateOf(1f)}
    var hardness by remember{mutableFloatStateOf(1f)}
    var straightLine by remember{mutableStateOf(false)}
    val colors=listOf(Color.White,Color.Black,Color(0xFFFF3B30),Color(0xFFFFC107),Color(0xFF00C853),Color(0xFF00B0FF),Color(0xFF7C4DFF),Color(0xFFFF4081))
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp)) {
            Text("Brush / Eraser",Modifier.weight(1f),color=Color.White)
            TextButton(onClick={if(strokes.isNotEmpty())strokes=strokes.dropLast(1)}){Text("Undo stroke")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(onClick={onApply(renderBrush(source,strokes+listOfNotNull(active),straightLine))}){Text("Apply")}
        }
        Canvas(Modifier.weight(1f).fillMaxWidth().aspectRatio(source.width.toFloat()/source.height).padding(12.dp).background(Color(0xFF25282D),RoundedCornerShape(8.dp))
            .pointerInput(source,brushSize,color,erase) {
                detectDragGestures(onDragStart={start->
                    active=BrushStroke(listOf(Offset(start.x/size.width,start.y/size.height)),color,brushSize,erase,opacity,hardness)
                },onDragEnd={active?.let{strokes=strokes+it};active=null},onDragCancel={active=null}) { change,_->
                    val normalized=Offset(change.position.x/size.width,change.position.y/size.height)
                    active=active?.copy(points=active!!.points+normalized)
                    change.consume()
                }
            }) {
            drawImage(source.asImageBitmap(),dstSize=androidx.compose.ui.unit.IntSize(size.width.toInt(),size.height.toInt()))
            (strokes+listOfNotNull(active)).forEach { s->
                if(s.points.isNotEmpty()) {
                    val path=Path().apply { moveTo(s.points.first().x*size.width,s.points.first().y*size.height);s.points.drop(1).forEach{lineTo(it.x*size.width,it.y*size.height)} }
                    if(s.points.size==1) drawCircle(if(s.erase) Color.Transparent else s.color.copy(alpha=s.opacity),radius=s.size*size.width/source.width/2f,center=Offset(s.points.first().x*size.width,s.points.first().y*size.height),blendMode=if(s.erase) androidx.compose.ui.graphics.BlendMode.Clear else androidx.compose.ui.graphics.BlendMode.SrcOver) else drawPath(path,if(s.erase) Color.Transparent else s.color.copy(alpha=s.opacity),style=Stroke(width=s.size*size.width/source.width,cap=androidx.compose.ui.graphics.StrokeCap.Round,join=androidx.compose.ui.graphics.StrokeJoin.Round),blendMode=if(s.erase) androidx.compose.ui.graphics.BlendMode.Clear else androidx.compose.ui.graphics.BlendMode.SrcOver)
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=!erase,onClick={erase=false},label={Text("Brush")})
                FilterChip(selected=erase,onClick={erase=true},label={Text("Eraser")})
                Text("Size ${brushSize.toInt()} px",color=Color.LightGray)
            }
            Slider(value=brushSize,onValueChange={brushSize=it},valueRange=2f..160f)
            Text("Opacity ${(opacity*100).toInt()}%",color=Color.LightGray);Slider(value=opacity,onValueChange={opacity=it},valueRange=0f..1f)
            Text("Hardness ${(hardness*100).toInt()}%",color=Color.LightGray);Slider(value=hardness,onValueChange={hardness=it},valueRange=0f..1f)
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Checkbox(checked=straightLine,onCheckedChange={straightLine=it});Text("Straight line",color=Color.White)}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { colors.forEach { c-> Button(onClick={color=c;erase=false},colors=ButtonDefaults.buttonColors(containerColor=c),modifier=Modifier.size(30.dp),contentPadding=PaddingValues(0.dp)){ } } }
        }
    }
}

private fun renderBrush(source:Bitmap,strokes:List<BrushStroke>,straightLine:Boolean):Bitmap {
    val output=source.copy(Bitmap.Config.ARGB_8888,true)
    val canvas=AndroidCanvas(output)
    strokes.forEach{stroke->
        if(stroke.points.isEmpty())return@forEach
        val points=if(straightLine&&stroke.points.size>1)listOf(stroke.points.first(),stroke.points.last())else stroke.points
        val paint=AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply{
            strokeCap=AndroidPaint.Cap.ROUND;strokeJoin=AndroidPaint.Join.ROUND;style=AndroidPaint.Style.STROKE
            strokeWidth=stroke.size;color=stroke.color.copy(alpha=stroke.opacity).toArgb()
            if(stroke.hardness<1f)maskFilter=BlurMaskFilter((1f-stroke.hardness)*stroke.size*.5f,BlurMaskFilter.Blur.NORMAL)
            if(stroke.erase)xfermode=PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        if(points.size==1)canvas.drawCircle(points.first().x*source.width,points.first().y*source.height,stroke.size/2f,paint)
        else {val path=android.graphics.Path().apply{moveTo(points.first().x*source.width,points.first().y*source.height);points.drop(1).forEach{lineTo(it.x*source.width,it.y*source.height)}};canvas.drawPath(path,paint)}
    }
    return output
}
