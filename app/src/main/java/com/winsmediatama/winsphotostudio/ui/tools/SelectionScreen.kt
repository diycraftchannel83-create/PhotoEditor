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

private enum class SelectionShape { RECTANGLE, ELLIPSE, POLYGON, FREEHAND, COLOR_RANGE, MAGIC_WAND }
enum class SelectionAction { CROP, MASK, COPY_LAYER }
data class SelectionOutput(val bitmap:Bitmap,val action:SelectionAction,val selectionMask:Bitmap?=null)

@Composable
fun SelectionScreen(source:Bitmap,onCancel:()->Unit,onApply:(SelectionOutput)->Unit) {
    var shape by remember{mutableStateOf(SelectionShape.RECTANGLE)}
    var action by remember{mutableStateOf(SelectionAction.CROP)}
    var rect by remember{mutableStateOf(Rect(.1f,.1f,.9f,.9f))}
    var dragStart by remember{mutableStateOf<Offset?>(null)}
    var polygonPoints by remember{mutableStateOf<List<Pair<Float,Float>>>(emptyList())}
    var sampleColor by remember{mutableStateOf(android.graphics.Color.WHITE)}
    var seedPoint by remember{mutableStateOf(0f to 0f)}
    var tolerance by remember{mutableFloatStateOf(.15f)}
    var softness by remember{mutableFloatStateOf(.1f)}
    var featherRadius by remember{mutableFloatStateOf(0f)}
    var expandRadius by remember{mutableFloatStateOf(0f)}
    var invertSelection by remember{mutableStateOf(false)}
    val selectionOverlay by produceState<Bitmap?>(null,source,shape,rect,polygonPoints,sampleColor,tolerance,softness,seedPoint,featherRadius,expandRadius,invertSelection){
        value?.takeIf{it!==source}?.recycle();value=null
        value=withContext(Dispatchers.Default){runCatching{buildSelectionOverlay(source,shape,rect,polygonPoints,sampleColor,tolerance,softness,seedPoint,featherRadius,expandRadius,invertSelection)}.getOrNull()}
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))){
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp)){
            Text("Selection",Modifier.weight(1f),color=Color.White)
            TextButton(onClick={rect=Rect(0f,0f,1f,1f);polygonPoints=emptyList()}){Text("Select all")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=(shape!=SelectionShape.POLYGON&&shape!=SelectionShape.FREEHAND)||(polygonPoints.size>=3),onClick={onApply(renderSelection(source,rect,shape,action,polygonPoints,sampleColor,tolerance,softness,seedPoint,featherRadius,expandRadius,invertSelection))}){Text("Apply")}
        }
        Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            SelectionShape.entries.forEach{s->FilterChip(selected=shape==s,onClick={shape=s},label={Text(s.name.replace("_"," "))})}
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            SelectionAction.entries.forEach{a->FilterChip(selected=action==a,onClick={action=a},label={Text(when(a){SelectionAction.CROP->"Crop";SelectionAction.MASK->"Mask";SelectionAction.COPY_LAYER->"Copy layer"})})}
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
            .pointerInput(shape){if(shape==SelectionShape.FREEHAND)detectDragGestures(onDragStart={start->polygonPoints=listOf((start.x/size.width).coerceIn(0f,1f) to (start.y/size.height).coerceIn(0f,1f))},onDragEnd={}){change,_->polygonPoints=polygonPoints+((change.position.x/size.width).coerceIn(0f,1f) to (change.position.y/size.height).coerceIn(0f,1f));change.consume()}}
            .pointerInput(shape){
                if(shape==SelectionShape.POLYGON||shape==SelectionShape.COLOR_RANGE||shape==SelectionShape.MAGIC_WAND) detectTapGestures{tap->
                    val nx=(tap.x/size.width).coerceIn(0f,1f);val ny=(tap.y/size.height).coerceIn(0f,1f)
                    if(shape==SelectionShape.POLYGON)polygonPoints=polygonPoints+(nx to ny)
                    else {seedPoint=nx to ny;sampleColor=source.getPixel((nx*source.width).toInt().coerceIn(0,source.width-1),(ny*source.height).toInt().coerceIn(0,source.height-1))}
                }
            }
        ){
            drawImage(source.asImageBitmap(),dstSize=androidx.compose.ui.unit.IntSize(size.width.toInt(),size.height.toInt()))
            selectionOverlay?.let{drawImage(it.asImageBitmap(),dstSize=androidx.compose.ui.unit.IntSize(size.width.toInt(),size.height.toInt()))}
            val box=Rect(rect.left*size.width,rect.top*size.height,rect.right*size.width,rect.bottom*size.height)
            if(shape==SelectionShape.RECTANGLE||shape==SelectionShape.ELLIPSE){val p=Path().apply{if(shape==SelectionShape.RECTANGLE)addRect(box)else addOval(box)};drawPath(p,Color(0xFF77D7C0),style=Stroke(3f))}
            if((shape==SelectionShape.POLYGON||shape==SelectionShape.FREEHAND)&&polygonPoints.size>=2){val p=Path().apply{moveTo(polygonPoints[0].first*size.width,polygonPoints[0].second*size.height);polygonPoints.drop(1).forEach{lineTo(it.first*size.width,it.second*size.height)};if(polygonPoints.size>=3)close()};drawPath(p,Color(0xFF77D7C0),style=Stroke(3f))}
            if(shape==SelectionShape.COLOR_RANGE||shape==SelectionShape.MAGIC_WAND){val marker=Offset(seedPoint.first*size.width,seedPoint.second*size.height);drawCircle(Color(sampleColor),radius=18f,center=marker);drawCircle(Color.White,radius=18f,center=marker,style=Stroke(3f))}
        }
        if(shape==SelectionShape.POLYGON||shape==SelectionShape.FREEHAND){Text(if(shape==SelectionShape.FREEHAND)"Lasso points: ${polygonPoints.size}" else "Polygon points: ${polygonPoints.size}",color=Color.LightGray);Button(onClick={polygonPoints=emptyList()}){Text("Clear polygon")}}
        if(shape==SelectionShape.COLOR_RANGE||shape==SelectionShape.MAGIC_WAND){Text(if(shape==SelectionShape.MAGIC_WAND)"Tap seed pixel for contiguous selection" else "Tap image to sample color",color=Color.LightGray);Text("Tolerance ${(tolerance*100).toInt()}%",color=Color.LightGray);Slider(value=tolerance,onValueChange={tolerance=it},valueRange=0f..1f);if(shape==SelectionShape.COLOR_RANGE){Text("Feather ${(softness*100).toInt()}%",color=Color.LightGray);Slider(value=softness,onValueChange={softness=it},valueRange=0f..1f)}}
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Checkbox(checked=invertSelection,onCheckedChange={invertSelection=it});Text("Invert selection",color=Color.White)}
        Text("Expand / contract ${expandRadius.toInt()} px",color=Color.LightGray);Slider(value=expandRadius,onValueChange={expandRadius=it},valueRange=-32f..32f)
        Text("Feather ${featherRadius.toInt()} px",color=Color.LightGray);Slider(value=featherRadius,onValueChange={featherRadius=it},valueRange=0f..32f)
        Text("Rectangle/ellipse: drag. Polygon: tap points. Freehand lasso: draw. Color range/magic wand: tap a sample. Crop or mask.",color=Color.LightGray,modifier=Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall)
    }
}

private fun buildSelectionOverlay(source:Bitmap,shape:SelectionShape,rect:Rect,points:List<Pair<Float,Float>>,sampleColor:Int,tolerance:Float,softness:Float,seed:Pair<Float,Float>,featherRadius:Float,expandRadius:Float,invertSelection:Boolean):Bitmap? {
    val processor=com.winsmediatama.winsphotostudio.core.tools.SelectionMaskProcessor
    val mask=when(shape){
        SelectionShape.RECTANGLE->processor.rectangle(source.width,source.height,rect.left,rect.top,rect.right,rect.bottom)
        SelectionShape.ELLIPSE->processor.ellipse(source.width,source.height,rect.left,rect.top,rect.right,rect.bottom)
        SelectionShape.POLYGON,SelectionShape.FREEHAND->if(points.size>=3)processor.polygon(source.width,source.height,points)else return null
        SelectionShape.COLOR_RANGE->processor.colorRange(source,sampleColor,tolerance,softness)
        SelectionShape.MAGIC_WAND->processor.magicWand(source,(seed.first*source.width).toInt(),(seed.second*source.height).toInt(),tolerance)
    }
    var processed=mask
    if(invertSelection)processed=processor.invert(processed)
    if(expandRadius.toInt()!=0){val next=processor.expandContract(processed,expandRadius.toInt());if(processed!==mask)processed.recycle();processed=next}
    if(featherRadius.toInt()>0){val next=processor.feather(processed,featherRadius.toInt());if(processed!==mask)processed.recycle();processed=next}
    val values=IntArray(source.width*source.height);processed.getPixels(values,0,source.width,0,0,source.width,source.height);if(processed!==mask)processed.recycle();mask.recycle()
    val overlay=IntArray(values.size){i->val alpha=(android.graphics.Color.red(values[i])*.3f).toInt().coerceIn(0,255);android.graphics.Color.argb(alpha,119,215,192)}
    return Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888).apply{setPixels(overlay,0,source.width,0,0,source.width,source.height)}
}
private fun Rect.normalize()=Rect(minOf(left,right),minOf(top,bottom),maxOf(left,right),maxOf(top,bottom))
private fun Rect.clampUnit()=Rect(left.coerceIn(0f,1f),top.coerceIn(0f,1f),right.coerceIn(0f,1f),bottom.coerceIn(0f,1f))

private fun renderSelection(source:Bitmap,selection:Rect,shape:SelectionShape,action:SelectionAction,polygonPoints:List<Pair<Float,Float>>,sampleColor:Int,tolerance:Float,softness:Float,seedPoint:Pair<Float,Float>,featherRadius:Float,expandRadius:Float,invertSelection:Boolean):SelectionOutput {
    val masks=com.winsmediatama.winsphotostudio.core.tools.SelectionMaskProcessor
    val mask=when(shape){
        SelectionShape.RECTANGLE->masks.rectangle(source.width,source.height,selection.left,selection.top,selection.right,selection.bottom)
        SelectionShape.ELLIPSE->masks.ellipse(source.width,source.height,selection.left,selection.top,selection.right,selection.bottom)
        SelectionShape.POLYGON,SelectionShape.FREEHAND->masks.polygon(source.width,source.height,polygonPoints)
        SelectionShape.COLOR_RANGE->masks.colorRange(source,sampleColor,tolerance,softness)
        SelectionShape.MAGIC_WAND->masks.magicWand(source,(seedPoint.first*source.width).toInt(),(seedPoint.second*source.height).toInt(),tolerance)
    }
    var processed=mask
    if(invertSelection)processed=masks.invert(processed)
    if(expandRadius.toInt()!=0)processed=masks.expandContract(processed,expandRadius.toInt())
    if(featherRadius.toInt()>0)processed=masks.feather(processed,featherRadius.toInt())
    if(action==SelectionAction.MASK)return SelectionOutput(applyMask(source,processed),action,processed)\n    if(action==SelectionAction.COPY_LAYER)return SelectionOutput(applyMask(source,processed),action)
    val pixels=IntArray(source.width*source.height);processed.getPixels(pixels,0,source.width,0,0,source.width,source.height)
    var minX=source.width;var minY=source.height;var maxX=-1;var maxY=-1
    for(y in 0 until source.height)for(x in 0 until source.width)if(android.graphics.Color.red(pixels[y*source.width+x])>0){minX=minOf(minX,x);minY=minOf(minY,y);maxX=maxOf(maxX,x);maxY=maxOf(maxY,y)}
    if(maxX<minX||maxY<minY)return SelectionOutput(source.copy(Bitmap.Config.ARGB_8888,true),action)\n    return SelectionOutput(Bitmap.createBitmap(source,minX,minY,maxX-minX+1,maxY-minY+1),action)
}
private fun applyMask(source:Bitmap,mask:Bitmap):Bitmap {
    val w=source.width;val h=source.height;val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h);val mp=IntArray(w*h);mask.getPixels(mp,0,w,0,0,w,h)
    for(i in pixels.indices){val p=pixels[i];val m=android.graphics.Color.red(mp[i]);val a=(p ushr 24)*m/255;pixels[i]=(a shl 24) or (p and 0x00FFFFFF)}
    return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
}
