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

private enum class LevelsTarget(val label:String) { RGB("RGB"), RED("Red"), GREEN("Green"), BLUE("Blue") }

@Composable
fun LevelsScreen(source: Bitmap, onCancel:()->Unit, onApply:(Bitmap)->Unit) {
    var target by remember { mutableStateOf(LevelsTarget.RGB) }
    var channel by remember { mutableStateOf(LevelsChannel()) }
    var composite by remember { mutableStateOf(LevelsChannel()) }
    var red by remember { mutableStateOf(LevelsChannel()) }
    var green by remember { mutableStateOf(LevelsChannel()) }
    var blue by remember { mutableStateOf(LevelsChannel()) }
    val values=LevelsValues(composite,red,green,blue)
    val preview by produceState<Bitmap?>(null,source,values) {
        value=withContext(Dispatchers.IO) { LevelsProcessor.apply(source,values) }
    }
    fun updateTarget(v:LevelsChannel) {
        when(target) {
            LevelsTarget.RGB -> composite=v
            LevelsTarget.RED -> red=v
            LevelsTarget.GREEN -> green=v
            LevelsTarget.BLUE -> blue=v
        }
    }
    channel=when(target) { LevelsTarget.RGB->composite; LevelsTarget.RED->red; LevelsTarget.GREEN->green; LevelsTarget.BLUE->blue }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Levels",Modifier.weight(1f),color=Color.White)
            TextButton(onClick={channel=LevelsChannel();updateTarget(channel)}){Text("Reset")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let(onApply)}){Text("Apply")}
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
