package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.adjustments.HslAdjustments
import com.winsmediatama.winsphotudio.core.adjustments.HslColorRange
import com.winsmediatama.winsphotostudio.core.adjustments.HslRangeAdjustment
import com.winsmediatama.winsphotostudio.core.adjustments.HslProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HslScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var selected by remember { mutableStateOf(HslColorRange.RED) }
    var ranges by remember { mutableStateOf(HslAdjustments()) }
    val current=ranges.ranges[selected] ?: HslRangeAdjustment()
    val preview by produceState<Bitmap?>(null,source,ranges) {
        value=null
        value=withContext(Dispatchers.IO){HslProcessor.apply(source,ranges)}
    }
    fun update(value:HslRangeAdjustment){ranges=HslAdjustments(ranges.ranges.toMutableMap().also{it[selected]=value})}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("HSL / Color Range",Modifier.weight(1f),color=Color.White)
            TextButton(onClick={update(HslRangeAdjustment())}){Text("Reset range")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let(onApply)}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center) {
            preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit)}?:CircularProgressIndicator()
        }
        LazyRow(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)),contentPadding=PaddingValues(8.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            items(HslColorRange.entries){range->FilterChip(selected=selected==range,onClick={selected=range},label={Text(range.label)})}
        }
        Column(Modifier.fillMaxWidth().heightIn(max=260.dp).background(Color(0xFF1B1E24)).padding(16.dp)) {
            HslControl("Hue",current.hueShift,-180f..180f){update(current.copy(hueShift=it))}
            HslControl("Saturation",current.saturation,-1f..1f){update(current.copy(saturation=it))}
            HslControl("Lightness",current.lightness,-1f..1f){update(current.copy(lightness=it))}
        }
    }
}

@Composable
private fun HslControl(label:String,value:Float,range:ClosedFloatingPointRange<Float>,onChange:(Float)->Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical=3.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=Color.White);Text("%.1f".format(value),color=Color.LightGray)}
        Slider(value=value,onValueChange=onChange,valueRange=range)
    }
}
