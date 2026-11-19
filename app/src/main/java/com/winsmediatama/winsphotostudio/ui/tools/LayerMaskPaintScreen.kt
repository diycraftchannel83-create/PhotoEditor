package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun LayerMaskPaintScreen(mask:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var working by remember(mask){mutableStateOf(mask.copy(Bitmap.Config.ARGB_8888,true))}
    var reveal by remember{mutableStateOf(true)}
    var radius by remember{mutableFloatStateOf(48f)}
    var revision by remember{mutableIntStateOf(0)}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))){
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
            Text("Paint Layer Mask",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(onClick={onApply(working)}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center){
            Image(remember(working,revision){working.asImageBitmap()},null,Modifier.fillMaxSize().pointerInput(working,reveal,radius){
                detectDragGestures(onDragStart={paintAt(it.x,it.y,size.width,size.height,working,reveal,radius);revision++},onDragEnd={},onDragCancel={}){change,_->paintAt(change.position.x,change.position.y,size.width,size.height,working,reveal,radius);revision++}
            },contentScale=ContentScale.Fit)
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=reveal,onClick={reveal=true},label={Text("Reveal (White)")});FilterChip(selected=!reveal,onClick={reveal=false},label={Text("Hide (Black)")})}
            Text("Brush size ${radius.toInt()} px",color=Color.LightGray);Slider(value=radius,onValueChange={radius=it},valueRange=8f..240f)
            Button(onClick={working.eraseColor(android.graphics.Color.WHITE);revision++},modifier=Modifier.fillMaxWidth()){Text("Reset mask to white")}
        }
    }
}
private fun paintAt(x:Float,y:Float,viewWidth:Int,viewHeight:Int,bitmap:Bitmap,reveal:Boolean,radius:Float) {
    if(viewWidth<=0||viewHeight<=0)return
    val scale=minOf(viewWidth.toFloat()/bitmap.width,viewHeight.toFloat()/bitmap.height)
    val drawWidth=bitmap.width*scale;val drawHeight=bitmap.height*scale
    val offsetX=(viewWidth-drawWidth)/2f;val offsetY=(viewHeight-drawHeight)/2f
    if(x<offsetX||y<offsetY||x>offsetX+drawWidth||y>offsetY+drawHeight)return
    val px=(x-offsetX)/scale;val py=(y-offsetY)/scale
    val paint=AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply{color=if(reveal)android.graphics.Color.WHITE else android.graphics.Color.BLACK;style=AndroidPaint.Style.FILL}
    AndroidCanvas(bitmap).drawCircle(px,py,radius,paint)
}
