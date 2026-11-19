package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OperationStackScreen(layer:EditorLayer,onCancel:()->Unit,onApply:(EditorLayer)->Unit) {
    val original=layer.originalBitmap?:layer.bitmap
    var operations by remember(layer.id,layer.operations){mutableStateOf(layer.operations)}
    val preview by produceState<Bitmap?>(null,original,operations){
        value?.takeIf{it!==original}?.recycle();value=null
        value=withContext(Dispatchers.IO){NonDestructiveEditStack(original,operations).render()}
    }
    androidx.compose.runtime.DisposableEffect(preview){onDispose{preview?.takeIf{it!==original}?.recycle()}}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))){
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp),verticalAlignment=Alignment.CenterVertically){Text("Non-destructive stack",Modifier.weight(1f),color=Color.White);TextButton(onClick=onCancel){Text("Cancel")};Button(enabled=preview!=null,onClick={preview?.let{onApply(layer.copy(bitmap=it.copy(Bitmap.Config.ARGB_8888,true),originalBitmap=original,operations=operations))}}){Text("Apply")}}
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center){preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit)}?:CircularProgressIndicator()}
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Text("Original source retained",color=Color.LightGray,modifier=Modifier.weight(1f));TextButton(onClick={operations=emptyList()}){Text("Reset")}}
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            itemsIndexed(operations,key={_,op->op.id}){index,op->
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF20242A))){Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){
                    Checkbox(checked=op.enabled,onCheckedChange={v->operations=operations.toMutableList().also{it[index]=toggle(it[index],v)}})
                    Text(op.label,Modifier.weight(1f),color=Color.White)
                    TextButton(enabled=index>0,onClick={operations=operations.toMutableList().also{val item=it.removeAt(index);it.add(index-1,item)}}){Text("Up")}
                    TextButton(enabled=index<operations.lastIndex,onClick={operations=operations.toMutableList().also{val item=it.removeAt(index);it.add(index+1,item)}}){Text("Down")}
                    TextButton(onClick={operations=operations.filterIndexed{i,_->i!=index}}){Text("Remove")}
                }}
            }
        }
        if(operations.isEmpty())Text("No operations. Apply an adjustment to create a replayable operation.",color=Color.LightGray,modifier=Modifier.padding(12.dp))
    }
}
private fun toggle(op:EditOperation,enabled:Boolean):EditOperation=when(op){
    is EditOperation.BasicAdjust->op.copy(enabled=enabled)
    is EditOperation.Levels->op.copy(enabled=enabled)
    is EditOperation.Curves->op.copy(enabled=enabled)
    is EditOperation.Hsl->op.copy(enabled=enabled)
    is EditOperation.Crop->op.copy(enabled=enabled)
    is EditOperation.Transform->op.copy(enabled=enabled)
    is EditOperation.Effect->op.copy(enabled=enabled)
    is EditOperation.Lut->op.copy(enabled=enabled)
    is EditOperation.Retouch->op.copy(enabled=enabled)
    is EditOperation.Perspective->op.copy(enabled=enabled)
    is EditOperation.CanvasSize->op.copy(enabled=enabled)
    is EditOperation.BackgroundReplace->op.copy(enabled=enabled)
}
