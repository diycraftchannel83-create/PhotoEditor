package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private enum class SelectionShape { RECTANGLE, ELLIPSE }
private enum class SelectionAction { CROP, MASK }

@Composable
fun SelectionScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var shape by remember { mutableStateOf(SelectionShape.RECTANGLE) }
    var action by remember { mutableStateOf(SelectionAction.CROP) }
    var rect by remember { mutableStateOf(Rect(.1f,.1f,.9f,.9f)) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp)) {
            Text("Selection",Modifier.weight(1f),color=Color.White)
            TextButton(onClick={rect=Rect(0f,0f,1f,1f)}){Text("Select all")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(onClick={onApply(renderSelection(source,rect,shape,action))}){Text("Apply")}
        }
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            SelectionShape.entries.forEach { s->FilterChip(selected=shape==s,onClick={shape=s},label={Text(if(s==SelectionShape.RECTANGLE)"Rectangle" else "Ellipse")}) }
            Spacer(Modifier.width(8.dp))
            SelectionAction.entries.forEach { a->FilterChip(selected=action==a,onClick={action=a},label={Text(if(a==SelectionAction.CROP)"Crop" else "Mask")}) }
        }
        Canvas(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D))
            .pointerInput(shape) {
                detectDragGestures(onDragStart={start->dragStart=Offset(start.x/size.width,start.y/size.height);rect=Rect(dragStart!!,dragStart!!)},onDragEnd={dragStart=null}) { change,_->
                    val start=dragStart ?: return@detectDragGestures
                    val end=Offset(change.position.x/size.width,change.position.y/size.height)
                    rect=Rect(start,end).normalize().clampUnit()
                    change.consume()
                }
            }) {
            drawImage(source.asImageBitmap(),dstSize=androidx.compose.ui.unit.IntSize(size.width.toInt(),size.height.toInt()))
            val r=Rect(rect.left*size.width,rect.top*size.height,rect.right*size.width,rect.bottom*size.height)
            val p=Path().apply { if(shape==SelectionShape.RECTANGLE) addRect(r) else addOval(r) }
            drawPath(p,Color(0xFF77D7C0),style=Stroke(width=3f))
        }
        Text("Drag to define selection. Crop reduces canvas; Mask keeps canvas and makes outside transparent.",color=Color.LightGray,modifier=Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall)
    }
}

private fun Rect.normalize()=Rect(minOf(left,right),minOf(top,bottom),maxOf(left,right),maxOf(top,bottom))
private fun Rect.clampUnit()=Rect(left.coerceIn(0f,1f),top.coerceIn(0f,1f),right.coerceIn(0f,1f),bottom.coerceIn(0f,1f))

private fun renderSelection(source:Bitmap,selection:Rect,shape:SelectionShape,action:SelectionAction):Bitmap {
    val l=(selection.left*source.width).roundToInt().coerceIn(0,source.width-1)
    val t=(selection.top*source.height).roundToInt().coerceIn(0,source.height-1)
    val r=(selection.right*source.width).roundToInt().coerceIn(l+1,source.width)
    val b=(selection.bottom*source.height).roundToInt().coerceIn(t+1,source.height)
    if(action==SelectionAction.CROP && shape==SelectionShape.RECTANGLE) return Bitmap.createBitmap(source,l,t,r-l,b-t)
    val output=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
    val canvas=AndroidCanvas(output)
    val path=AndroidPath()
    if(shape==SelectionShape.RECTANGLE) path.addRect(l.toFloat(),t.toFloat(),r.toFloat(),b.toFloat(),AndroidPath.Direction.CW)
    else path.addOval(l.toFloat(),t.toFloat(),r.toFloat(),b.toFloat(),AndroidPath.Direction.CW)
    canvas.clipPath(path)
    canvas.drawBitmap(source,0f,0f,AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG))
    if(action==SelectionAction.CROP) {
        return Bitmap.createBitmap(output,l,t,r-l,b-t).also { output.recycle() }
    }
    return output
}
