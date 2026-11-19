package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.unit.dp

private enum class SelectionShape { RECTANGLE, ELLIPSE, POLYGON, COLOR_RANGE, MAGIC_WAND }
private enum class SelectionAction { CROP, MASK }

@Composable
fun SelectionScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var shape by remember{mutableStateOf(SelectionShape.RECTANGLE)}
    var action by remember{mutableStateOf(SelectionAction.CROP)}
    var rect by remember{mutableStateOf(Rect(.1f,.1f,.9f,.9f))}
    var dragStart by remember{mutableStateOf<Offset?>(null)}
    var polygonPoints by remember{mutableStateOf<List<Pair<Float,Float>>>(emptyList())}
    var sampleColor by remember{mutableStateOf(android.graphics.Color.WHITE)}
    var seedPoint by remember{mutableStateOf(0f to 0f)}
    var tolerance by remember{mutableFloatStateOf(.15f)}
    var softness by remember{mutableFloatStateOf(.1f)}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))){
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp)){
            Text("Selection",Modifier.weight(1f),color=Color.White)
            TextButton(onClick={rect=Rect(0f,0f,1f,1f);polygonPoints=emptyList()}){Text("Select all")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=shape!=SelectionShape.POLYGON||polygonPoints.size>=3,onClick={onApply(renderSelection(source,rect,shape,action,polygonPoints,sampleColor,tolerance,softness,seedPoint))}){Text("Apply")}
        }
        Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            SelectionShape.entries.forEach{s->FilterChip(selected=shape==s,onClick={shape=s},label={Text(s.name.replace("_"," "))})}
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            SelectionAction.entries.forEach{a->FilterChip(selected=action==a,onClick={action=a},label={Text(a.name)})}
        }
        Canvas(Modifier.weight(1f).fillMaxWidth().aspectRatio(source.width.toFloat()/source.height).padding(12.dp).background(Color(0xFF25282D))
            .pointerInput(shape){
                if(shape==SelectionShape.RECTANGLE||shape==SelectionShape.ELLIPSE) detectDragGestures(
                    onDragStart={start->dragStart=Offset(start.x/size.width,start.y/size.height);rect=Rect(dragStart!!.x,dragStart!!.y,dragStart!!.x,dragStart!!.y)},
                    onDragEnd={dragStart=null}
                ){change,_->
                    val start=dragStart?:return@detectDragGestures
                    val end=Offset(change.position.x/size.width,change.position.y/size.height)
                    rect=Rect(start.x,start.y,end.x,end.y).normalize().clampUnit();change.consume()
                }
            }
            .pointerInput(shape){
                if(shape==SelectionShape.POLYGON||shape==SelectionShape.COLOR_RANGE||shape==SelectionShape.MAGIC_WAND) detectTapGestures{tap->
                    val nx=(tap.x/size.width).coerceIn(0f,1f);val ny=(tap.y/size.height).coerceIn(0f,1f)
                    if(shape==SelectionShape.POLYGON)polygonPoints=polygonPoints+(nx to ny)
                    else {seedPoint=nx to ny;sampleColor=source.getPixel((nx*source.width).toInt().coerceIn(0,source.width-1),(ny*source.height).toInt().coerceIn(0,source.height-1)}
                }
            }
        ){
            drawImage(source.asImageBitmap(),dstSize=androidx.compose.ui.unit.IntSize(size.width.toInt(),size.height.toInt()))
            val box=Rect(rect.left*size.width,rect.top*size.height,rect.right*size.width,rect.bottom*size.height)
            if(shape==SelectionShape.RECTANGLE||shape==SelectionShape.ELLIPSE){val p=Path().apply{if(shape==SelectionShape.RECTANGLE)addRect(box)else addOval(box)};drawPath(p,Color(0xFF77D7C0),style=Stroke(3f))}
            if(shape==SelectionShape.POLYGON&&polygonPoints.size>=2){val p=Path().apply{moveTo(polygonPoints[0].first*size.width,polygonPoints[0].second*size.height);polygonPoints.drop(1).forEach{lineTo(it.first*size.width,it.second*size.height)};if(polygonPoints.size>=3)close()};drawPath(p,Color(0xFF77D7C0),style=Stroke(3f))}
            if(shape==SelectionShape.COLOR_RANGE||shape==SelectionShape.MAGIC_WAND){val marker=Offset(seedPoint.first*size.width,seedPoint.second*size.height);drawCircle(Color(sampleColor),radius=18f,center=marker);drawCircle(Color.White,radius=18f,center=marker,style=Stroke(3f))}
        }
        if(shape==SelectionShape.POLYGON){Text("Points: ${polygonPoints.size}",color=Color.LightGray);Button(onClick={polygonPoints=emptyList()}){Text("Clear polygon")}}
        if(shape==SelectionShape.COLOR_RANGE||shape==SelectionShape.MAGIC_WAND){Text(if(shape==SelectionShape.MAGIC_WAND)"Tap seed pixel for contiguous selection" else "Tap image to sample color",color=Color.LightGray);Text("Tolerance ${(tolerance*100).toInt()}%",color=Color.LightGray);Slider(value=tolerance,onValueChange={tolerance=it},valueRange=0f..1f);if(shape==SelectionShape.COLOR_RANGE){Text("Feather ${(softness*100).toInt()}%",color=Color.LightGray);Slider(value=softness,onValueChange={softness=it},valueRange=0f..1f)}}
        Text("Rectangle/ellipse: drag. Polygon: tap points. Color range/magic wand: tap a sample. Crop or mask.",color=Color.LightGray,modifier=Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall)
    }
}

private fun Rect.normalize()=Rect(minOf(left,right),minOf(top,bottom),maxOf(left,right),maxOf(top,bottom))
private fun Rect.clampUnit()=Rect(left.coerceIn(0f,1f),top.coerceIn(0f,1f),right.coerceIn(0f,1f),bottom.coerceIn(0f,1f))

private fun renderSelection(source:Bitmap,selection:Rect,shape:SelectionShape,action:SelectionAction,polygonPoints:List<Pair<Float,Float>>,sampleColor:Int,tolerance:Float,softness:Float,seedPoint:Pair<Float,Float>):Bitmap {
    val masks=com.winsmediatama.winsphotostudio.core.tools.SelectionMaskProcessor
    val mask=when(shape){
        SelectionShape.RECTANGLE->masks.rectangle(source.width,source.height,selection.left,selection.top,selection.right,selection.bottom)
        SelectionShape.ELLIPSE->masks.ellipse(source.width,source.height,selection.left,selection.top,selection.right,selection.bottom)
        SelectionShape.POLYGON->masks.polygon(source.width,source.height,polygonPoints)
        SelectionShape.COLOR_RANGE->masks.colorRange(source,sampleColor,tolerance,softness)
        SelectionShape.MAGIC_WAND->masks.magicWand(source,(seedPoint.first*source.width).toInt(),(seedPoint.second*source.height).toInt(),tolerance)
    }
    if(action==SelectionAction.MASK)return applyMask(source,mask)
    val pixels=IntArray(source.width*source.height);mask.getPixels(pixels,0,source.width,0,0,source.width,source.height)
    var minX=source.width;var minY=source.height;var maxX=-1;var maxY=-1
    for(y in 0 until source.height)for(x in 0 until source.width)if(android.graphics.Color.red(pixels[y*source.width+x])>0){minX=minOf(minX,x);minY=minOf(minY,y);maxX=maxOf(maxX,x);maxY=maxOf(maxY,y)}
    if(maxX<minX||maxY<minY)return source.copy(Bitmap.Config.ARGB_8888,true)
    return Bitmap.createBitmap(source,minX,minY,maxX-minX+1,maxY-minY+1)
}
private fun applyMask(source:Bitmap,mask:Bitmap):Bitmap {
    val w=source.width;val h=source.height;val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h);val mp=IntArray(w*h);mask.getPixels(mp,0,w,0,0,w,h)
    for(i in pixels.indices){val p=pixels[i];val m=android.graphics.Color.red(mp[i]);val a=(p ushr 24)*m/255;pixels[i]=(a shl 24) or (p and 0x00FFFFFF)}
    return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
}
