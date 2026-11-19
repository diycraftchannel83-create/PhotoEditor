package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb

private enum class OverlayKind(val title:String) { TEXT("Text"), RECTANGLE("Rectangle"), ELLIPSE("Ellipse"), LINE("Line") }

@Composable
fun TextShapeScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var kind by remember { mutableStateOf(OverlayKind.TEXT) }
    var text by remember { mutableStateOf("WinsPhoto Studio") }
    var color by remember { mutableStateOf(Color.White) }
    var fontSize by remember { mutableFloatStateOf(72f) }
    var x by remember { mutableFloatStateOf(.5f) }
    var y by remember { mutableFloatStateOf(.5f) }
    var opacity by remember { mutableFloatStateOf(1f) }
    val palette=listOf(Color.White,Color.Black,Color(0xFFFF3B30),Color(0xFFFFC107),Color(0xFF00C853),Color(0xFF00B0FF),Color(0xFF7C4DFF),Color(0xFFFF4081))
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp)) {
            Text("Text & Shapes",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(onClick={onApply(renderOverlay(source,kind,text,color,fontSize,x,y,opacity))}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().aspectRatio(source.width.toFloat()/source.height).padding(12.dp).background(Color(0xFF25282D)) {
            Image(source.asImageBitmap(),null,Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                val nc=drawContext.canvas.nativeCanvas
                val p=AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { color=color.copy(alpha=opacity).toArgb();textSize=fontSize*size.width/source.width;strokeWidth=(fontSize*size.width/source.width).coerceAtLeast(2f);style=AndroidPaint.Style.STROKE }
                val cx=x*size.width;val cy=y*size.height
                when(kind) {
                    OverlayKind.TEXT -> { p.style=AndroidPaint.Style.FILL;nc.drawText(text,cx,cy,p) }
                    OverlayKind.RECTANGLE -> nc.drawRect(cx-size.width*.12f,cy-size.height*.08f,cx+size.width*.12f,cy+size.height*.08f,p)
                    OverlayKind.ELLIPSE -> nc.drawOval(RectF(cx-size.width*.12f,cy-size.height*.08f,cx+size.width*.12f,cy+size.height*.08f),p)
                    OverlayKind.LINE -> nc.drawLine(cx-size.width*.15f,cy,cx+size.width*.15f,cy,p)
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { OverlayKind.entries.forEach{k->FilterChip(selected=kind==k,onClick={kind=k},label={Text(k.title)})} }
            if(kind==OverlayKind.TEXT) OutlinedTextField(value=text,onValueChange={text=it},label={Text("Text")},modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { palette.forEach { c->Button(onClick={color=c},colors=ButtonDefaults.buttonColors(containerColor=c),modifier=Modifier.size(30.dp),contentPadding=PaddingValues(0.dp)){} } }
            Text("Size ${fontSize.toInt()} px",color=Color.LightGray);Slider(value=fontSize,onValueChange={fontSize=it},valueRange=12f..240f)
            Text("Horizontal position ${(x*100).toInt()}%",color=Color.LightGray);Slider(value=x,onValueChange={x=it},valueRange=0f..1f)
            Text("Vertical position ${(y*100).toInt()}%",color=Color.LightGray);Slider(value=y,onValueChange={y=it},valueRange=0f..1f)
            Text("Opacity ${(opacity*100).toInt()}%",color=Color.LightGray);Slider(value=opacity,onValueChange={opacity=it},valueRange=0f..1f)
        }
    }
}

private fun renderOverlay(source:Bitmap,kind:OverlayKind,text:String,color:Color,fontSize:Float,x:Float,y:Float,opacity:Float):Bitmap {
    val out=source.copy(Bitmap.Config.ARGB_8888,true);val canvas=AndroidCanvas(out)
    val p=AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply { this.color=color.copy(alpha=opacity).toArgb();textSize=fontSize;strokeWidth=(fontSize/24f).coerceAtLeast(2f);style=AndroidPaint.Style.STROKE }
    val cx=x*source.width;val cy=y*source.height
    when(kind) {
        OverlayKind.TEXT->{p.style=AndroidPaint.Style.FILL;canvas.drawText(text,cx,cy,p)}
        OverlayKind.RECTANGLE->canvas.drawRect(cx-source.width*.12f,cy-source.height*.08f,cx+source.width*.12f,cy+source.height*.08f,p)
        OverlayKind.ELLIPSE->canvas.drawOval(RectF(cx-source.width*.12f,cy-source.height*.08f,cx+source.width*.12f,cy+source.height*.08f),p)
        OverlayKind.LINE->canvas.drawLine(cx-source.width*.15f,cy,cx+source.width*.15f,cy,p)
    }
    return out
}
