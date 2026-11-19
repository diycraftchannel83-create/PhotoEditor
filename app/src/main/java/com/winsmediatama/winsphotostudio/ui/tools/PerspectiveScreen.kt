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
import com.winsmediatama.winsphotostudio.core.tools.PerspectiveQuad
import com.winsmediatama.winsphotostudio.core.tools.PerspectiveTransformProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PerspectiveScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var tlx by remember{mutableFloatStateOf(0f)};var tly by remember{mutableFloatStateOf(0f)}
    var trx by remember{mutableFloatStateOf(1f)};var tryy by remember{mutableFloatStateOf(0f)}
    var brx by remember{mutableFloatStateOf(1f)};var bry by remember{mutableFloatStateOf(1f)}
    var blx by remember{mutableFloatStateOf(0f)};var bly by remember{mutableFloatStateOf(1f)}
    val quad=PerspectiveQuad(tlx to tly,trx to tryy,brx to bry,blx to bly)
    val preview by produceState<Bitmap?>(null,source,quad){value?.takeIf{it!==source}?.recycle();value=null;if(quad.isValid())value=withContext(Dispatchers.Default){runCatching{PerspectiveTransformProcessor.warp(source,quad)}.getOrNull()}}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))){
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text("Perspective",Modifier.weight(1f),color=Color.White);TextButton(onClick=onCancel){Text("Cancel")};Button(enabled=preview!=null,onClick={preview?.let(onApply)}){Text("Apply")}}
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center){preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit)}?:Text("Adjust corners to correct perspective",color=Color.LightGray)}
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)){
            CornerControl("Top left",tlx,tly,{tlx=it},{tly=it})
            CornerControl("Top right",trx,tryy,{trx=it},{tryy=it})
            CornerControl("Bottom right",brx,bry,{brx=it},{bry=it})
            CornerControl("Bottom left",blx,bly,{blx=it},{bly=it})
            Button(onClick={tlx=0f;tly=0f;trx=1f;tryy=0f;brx=1f;bry=1f;blx=0f;bly=1f},modifier=Modifier.fillMaxWidth()){Text("Reset")}
        }
    }
}
@Composable private fun CornerControl(label:String,x:Float,y:Float,setX:(Float)->Unit,setY:(Float)->Unit){
    Text("$label X: ${(x*100).toInt()}%",color=Color.LightGray,style=MaterialTheme.typography.labelSmall);Slider(value=x,onValueChange=setX,valueRange=0f..1f)
    Text("$label Y: ${(y*100).toInt()}%",color=Color.LightGray,style=MaterialTheme.typography.labelSmall);Slider(value=y,onValueChange=setY,valueRange=0f..1f)
}
