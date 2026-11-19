package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.adjustments.CurvePoint
import com.winsmediatama.winsphotostudio.core.adjustments.CurvesProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.CurvesValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class CurveChannel(val title:String) { RGB("RGB"), RED("Red"), GREEN("Green"), BLUE("Blue") }
private fun identityCurve()=listOf(CurvePoint(0f,0f),CurvePoint(.25f,.25f),CurvePoint(.5f,.5f),CurvePoint(.75f,.75f),CurvePoint(1f,1f))

@Composable
fun CurvesScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var target by remember { mutableStateOf(CurveChannel.RGB) }
    var rgb by remember { mutableStateOf(identityCurve()) }
    var red by remember { mutableStateOf(identityCurve()) }
    var green by remember { mutableStateOf(identityCurve()) }
    var blue by remember { mutableStateOf(identityCurve()) }
    var selected by remember { mutableIntStateOf(2) }
    val current=when(target){CurveChannel.RGB->rgb;CurveChannel.RED->red;CurveChannel.GREEN->green;CurveChannel.BLUE->blue}
    val curveValues=CurvesValues(composite=rgb,red=red,green=green,blue=blue)
    fun setCurve(list:List<CurvePoint>) { when(target){CurveChannel.RGB->rgb=list;CurveChannel.RED->red=list;CurveChannel.GREEN->green=list;CurveChannel.BLUE->blue=list} }
    val preview by produceState<Bitmap?>(null,source,curveValues) { value?.takeIf { it !== source }?.recycle(); value=null; value=withContext(Dispatchers.IO){CurvesProcessor.apply(source,curveValues)} }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Curves",Modifier.weight(1f),color=Color.White)
            TextButton(onClick={setCurve(identityCurve())}){Text("Reset")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let(onApply)}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center) {
            preview?.let { Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit) } ?: CircularProgressIndicator()
        }
        Column(Modifier.fillMaxWidth().heightIn(max=360.dp).background(Color(0xFF1B1E24)).padding(16.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { CurveChannel.entries.forEach { ch-> FilterChip(selected=target==ch,onClick={target=ch;selected=2},label={Text(ch.title)}) } }
            Canvas(Modifier.fillMaxWidth().height(220.dp).padding(8.dp)
                .pointerInput(target) { detectTapGestures { pos ->
                    val pad=8f; val size=this.size; val nx=((pos.x-pad)/(size.width-2*pad)).coerceIn(0f,1f)
                    selected=(1..3).minByOrNull { kotlin.math.abs(current[it].x-nx) } ?: 2
                } }
                .pointerInput(target,selected) { detectDragGestures { change, _ ->
                    change.consume()
                    val pad=8f; val w=size.width-2*pad; val h=size.height-2*pad
                    val y=((change.position.y-pad)/h).coerceIn(0f,1f)
                    val updated=current.toMutableList();updated[selected]=updated[selected].copy(y=1f-y);setCurve(updated)
                } }
            ) {
                val pad=8f; val w=size.width-2*pad; val h=size.height-2*pad
                for(i in 1..3) { val x=pad+w*i/4; val y=pad+h*i/4; drawLine(Color.DarkGray,Offset(x,pad),Offset(x,pad+h),1f);drawLine(Color.DarkGray,Offset(pad,y),Offset(pad+w,y),1f) }
                drawLine(Color.Gray,Offset(pad,pad+h),Offset(pad+w,pad),2f)
                val path=Path()
                current.forEachIndexed { i,p ->
                    val pos=Offset(pad+p.x*w,pad+(1f-p.y)*h)
                    if(i==0) path.moveTo(pos.x,pos.y) else path.lineTo(pos.x,pos.y)
                }
                drawPath(path,Color(0xFF77D7C0),3f)
                current.forEachIndexed { i,p ->
                    val pos=Offset(pad+p.x*w,pad+(1f-p.y)*h)
                    drawCircle(if(i==selected) Color.White else Color(0xFF77D7C0),if(i==selected) 9f else 6f,pos)
                }
            }
            Text("Drag control points to edit the curve. Select RGB or individual channels.",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
        }
    }
}
