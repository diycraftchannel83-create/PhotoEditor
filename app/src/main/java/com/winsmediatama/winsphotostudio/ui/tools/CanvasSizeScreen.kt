package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.tools.CanvasAnchor
import com.winsmediatama.winsphotostudio.core.tools.CanvasResizeProcessor

@Composable
fun CanvasSizeScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var width by remember{mutableFloatStateOf(source.width.toFloat())}
    var height by remember{mutableFloatStateOf(source.height.toFloat())}
    var anchor by remember{mutableStateOf(CanvasAnchor.CENTER)}
    var background by remember{mutableStateOf(Color.Transparent)}
    val anchors=CanvasAnchor.entries
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))){
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
            Text("Canvas Size",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(onClick={onApply(CanvasResizeProcessor.resize(source,width.toInt(),height.toInt(),anchor,background.toArgb()))}){Text("Apply")}
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)){
            Text("Width: ${width.toInt()} px",color=Color.LightGray);Slider(value=width,onValueChange={width=it},valueRange=1f..minOf(8192f,source.width*3f).coerceAtLeast(1f))
            Text("Height: ${height.toInt()} px",color=Color.LightGray);Slider(value=height,onValueChange={height=it},valueRange=1f..minOf(8192f,source.height*3f).coerceAtLeast(1f))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={width=source.width.toFloat();height=source.height.toFloat()}){Text("Reset")};Button(onClick={val t=width;width=height;height=t}){Text("Swap")}}
            Text("Anchor",color=Color.White)
            anchors.chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){row.forEach{a->FilterChip(selected=anchor==a,onClick={anchor=a},label={Text(a.name.replace("_"," "),maxLines=1)})}}}
            Text("Background",color=Color.White)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(Color.Transparent,Color.White,Color.Black).forEach{c->Button(onClick={background=c},colors=ButtonDefaults.buttonColors(containerColor=if(c==Color.Transparent)Color(0xFF555555) else c),modifier=Modifier.size(40.dp),contentPadding=PaddingValues(0.dp)){if(background==c)Text("✓",color=if(c==Color.Black)Color.White else Color.Black)}}}
        }
    }
}
