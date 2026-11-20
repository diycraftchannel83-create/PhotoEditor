package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.adjustments.LevelsChannel
import com.winsmediatama.winsphotostudio.core.adjustments.LevelsProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.LevelsValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import com.winsmediatama.winsphotostudio.core.render.BitmapRenderScheduler
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

private enum class LevelsTarget(val label:String) { RGB("RGB"), RED("Red"), GREEN("Green"), BLUE("Blue") }

@Composable
fun LevelsScreen(source: Bitmap, onCancel:()->Unit, onApply:(Bitmap,LevelsValues)->Unit) {
    var target by remember { mutableStateOf(LevelsTarget.RGB) }
    var composite by remember { mutableStateOf(LevelsChannel()) }
    var red by remember { mutableStateOf(LevelsChannel()) }
    var green by remember { mutableStateOf(LevelsChannel()) }
    var blue by remember { mutableStateOf(LevelsChannel()) }
    val values=LevelsValues(composite,red,green,blue)
    val renderScheduler=remember{BitmapRenderScheduler(0L,"WinsPhoto-LevelsPreview")}
    val previewBitmaps=remember{mutableListOf<Bitmap>()}
    DisposableEffect(renderScheduler){onDispose{renderScheduler.close();previewBitmaps.toList().forEach{if(!it.isRecycled)it.recycle()};previewBitmaps.clear()}}
    val preview by produceState<Bitmap?>(null,source,values,renderScheduler) {
        value=null
        val key="levels:"+System.identityHashCode(source)+":"+source.width+"x"+source.height+":"+values.hashCode()
        var request:Future<Bitmap>?=null
        try{value=withContext(Dispatchers.IO){request=renderScheduler.request(key){signal->signal.throwIfCanceled();LevelsProcessor.apply(source,values,signal).also{signal.throwIfCanceled()}};request!!.get(60,TimeUnit.SECONDS)}?.also{previewBitmaps.add(it)}}
        catch(cancelled:CancellationException){request?.cancel(true);throw cancelled}
        catch(_:Exception){request?.cancel(true);value=null}
    }
    fun updateTarget(v:LevelsChannel) {
        when(target) {
            LevelsTarget.RGB -> composite=v
            LevelsTarget.RED -> red=v
            LevelsTarget.GREEN -> green=v
            LevelsTarget.BLUE -> blue=v
        }
    }
    val channel=when(target) { LevelsTarget.RGB->composite; LevelsTarget.RED->red; LevelsTarget.GREEN->green; LevelsTarget.BLUE->blue }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Levels",Modifier.weight(1f),color=Color.White)
            TextButton(onClick={updateTarget(LevelsChannel())}){Text("Reset")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let{onApply(it.copy(Bitmap.Config.ARGB_8888,true),values)}}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center) {
            preview?.let { Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit) } ?: CircularProgressIndicator()
        }
        Column(Modifier.fillMaxWidth().heightIn(max=390.dp).background(Color(0xFF1B1E24)).padding(16.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                LevelsTarget.entries.forEach { t -> FilterChip(selected=target==t,onClick={target=t},label={Text(t.label)}) }
            }
            LevelsControl("Input Black",channel.inputBlack,0f..0.95f){updateTarget(channel.copy(inputBlack=it.coerceAtMost(channel.inputWhite-0.01f)))}
            LevelsControl("Gamma",channel.inputGamma,0.1f..3f){updateTarget(channel.copy(inputGamma=it))}
            LevelsControl("Input White",channel.inputWhite,0.05f..1f){updateTarget(channel.copy(inputWhite=it.coerceAtLeast(channel.inputBlack+0.01f)))}
            LevelsControl("Output Black",channel.outputBlack,0f..1f){updateTarget(channel.copy(outputBlack=it.coerceAtMost(channel.outputWhite)))}
            LevelsControl("Output White",channel.outputWhite,0f..1f){updateTarget(channel.copy(outputWhite=it.coerceAtLeast(channel.outputBlack)))}
        }
    }
}

@Composable
private fun LevelsControl(label:String,value:Float,range:ClosedFloatingPointRange<Float>,onChange:(Float)->Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical=2.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(label,color=Color.White); Text("%.3f".format(value),color=Color.LightGray) }
        Slider(value=value,onValueChange=onChange,valueRange=range)
    }
}
