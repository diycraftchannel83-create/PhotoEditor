package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@Composable
fun BackgroundRemoveScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var sample by remember { mutableStateOf(Color(0f,0f,0f)) }
    var samplePoint by remember { mutableStateOf(androidx.compose.ui.geometry.Offset(.02f,.02f)) }
    var tolerance by remember { mutableFloatStateOf(35f) }
    var feather by remember { mutableFloatStateOf(20f) }
    var sampled by remember { mutableStateOf(false) }
    val preview by produceState<Bitmap?>(null,source,sample,tolerance,feather,sampled) {
        value=withContext(Dispatchers.IO){if(sampled) removeColorBackground(source,sample,tolerance,feather) else source}
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp)) {
            Text("Background Remove",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=sampled&&preview!=null,onClick={preview?.let(onApply)}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D))) {
            preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)}
            Canvas(Modifier.fillMaxSize().pointerInput(source) { detectTapGestures { point->
                val x=(point.x/size.width*source.width).toInt().coerceIn(0,source.width-1)
                val y=(point.y/size.height*source.height).toInt().coerceIn(0,source.height-1)
                sample=Color(source.getPixel(x,y));samplePoint=androidx.compose.ui.geometry.Offset(point.x/size.width,point.y/size.height);sampled=true
            } }) {
                if(sampled) drawCircle(Color.White,10f,androidx.compose.ui.geometry.Offset(samplePoint.x*size.width,samplePoint.y*size.height))
            }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)) {
            Text("Tap the background color to sample. Works best with a plain or high-contrast background.",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
            Text("Tolerance ${tolerance.toInt()}",color=Color.White);Slider(value=tolerance,onValueChange={tolerance=it},valueRange=5f..150f)
            Text("Feather ${feather.toInt()}",color=Color.White);Slider(value=feather,onValueChange={feather=it},valueRange=0f..100f)
            Button(onClick={sampled=false},modifier=Modifier.fillMaxWidth()){Text("Reset sample")}
        }
    }
}

private fun removeColorBackground(source:Bitmap,sample:Color,tolerance:Float,feather:Float):Bitmap {
    val out=source.copy(Bitmap.Config.ARGB_8888,true)
    val pixels=IntArray(source.width*source.height);out.getPixels(pixels,0,source.width,0,0,source.width,source.height)
    val sr=sample.red*255f;val sg=sample.green*255f;val sb=sample.blue*255f
    for(i in pixels.indices) {
        val p=pixels[i];val r=((p shr 16)and 255).toFloat();val g=((p shr 8)and 255).toFloat();val b=(p and 255).toFloat()
        val d=sqrt((r-sr)*(r-sr)+(g-sg)*(g-sg)+(b-sb)*(b-sb))
        val alpha=if(feather<=0f)if(d<=tolerance)0f else 1f else ((d-tolerance)/feather).coerceIn(0f,1f)
        pixels[i]=(alpha*255f).toInt().shl(24) or (p and 0x00FFFFFF)
    }
    out.setPixels(pixels,0,source.width,0,0,source.width,source.height)
    return out
}
