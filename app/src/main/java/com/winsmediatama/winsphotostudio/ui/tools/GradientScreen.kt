package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Paint as AndroidPaint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class GradientKind { LINEAR, RADIAL }

@Composable
fun GradientScreen(canvasWidth:Int,canvasHeight:Int,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var kind by remember { mutableStateOf(GradientKind.LINEAR) }
    var startColor by remember { mutableStateOf(Color(0xFF77D7C0)) }
    var endColor by remember { mutableStateOf(Color(0xFF102A43)) }
    var angle by remember { mutableFloatStateOf(45f) }
    var opacity by remember { mutableFloatStateOf(1f) }
    val colors=listOf(Color.White,Color.Black,Color(0xFFFF3B30),Color(0xFFFFC107),Color(0xFF00C853),Color(0xFF00B0FF),Color(0xFF7C4DFF),Color(0xFFFF4081),Color(0xFF77D7C0))
    val preview by produceState<Bitmap?>(null,canvasWidth,canvasHeight,kind,startColor,endColor,angle,opacity) {
        value=null
        value=withContext(Dispatchers.IO){createGradient(canvasWidth,canvasHeight,kind,startColor,endColor,angle,opacity)}
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Gradient Layer",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let(onApply)}){Text("Add layer")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center) {
            preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit)}?:CircularProgressIndicator()
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){GradientKind.entries.forEach{k->FilterChip(selected=kind==k,onClick={kind=k},label={Text(if(k==GradientKind.LINEAR)"Linear" else "Radial")})}}
            Text("Start color",color=Color.White);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){colors.forEach{c->Button(onClick={startColor=c},colors=ButtonDefaults.buttonColors(containerColor=c),modifier=Modifier.size(28.dp),contentPadding=PaddingValues(0.dp)){} }}
            Text("End color",color=Color.White);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){colors.forEach{c->Button(onClick={endColor=c},colors=ButtonDefaults.buttonColors(containerColor=c),modifier=Modifier.size(28.dp),contentPadding=PaddingValues(0.dp)){} }}
            if(kind==GradientKind.LINEAR){Text("Angle ${angle.toInt()}°",color=Color.LightGray);Slider(value=angle,onValueChange={angle=it},valueRange=0f..360f)}
            Text("Opacity ${(opacity*100).toInt()}%",color=Color.LightGray);Slider(value=opacity,onValueChange={opacity=it},valueRange=0f..1f)
        }
    }
}

private fun createGradient(width:Int,height:Int,kind:GradientKind,start:Color,end:Color,angle:Float,opacity:Float):Bitmap {
    val bitmap=Bitmap.createBitmap(width.coerceAtLeast(1),height.coerceAtLeast(1),Bitmap.Config.ARGB_8888)
    val canvas=AndroidCanvas(bitmap)
    val paint=AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { alpha=(opacity.coerceIn(0f,1f)*255).toInt() }
    if(kind==GradientKind.LINEAR) {
        val rad=Math.toRadians(angle.toDouble())
        val dx=(kotlin.math.cos(rad)*width/2).toFloat();val dy=(kotlin.math.sin(rad)*height/2).toFloat()
        paint.shader=LinearGradient(width/2f-dx,height/2f-dy,width/2f+dx,height/2f+dy,start.toArgb(),end.toArgb(),Shader.TileMode.CLAMP)
    } else {
        paint.shader=RadialGradient(width/2f,height/2f,maxOf(width,height)/2f,start.toArgb(),end.toArgb(),Shader.TileMode.CLAMP)
    }
    canvas.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
    paint.shader=null
    return bitmap
}
