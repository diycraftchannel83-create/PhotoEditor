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
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OperationStackScreen(layer:EditorLayer,onCancel:()->Unit,onApply:(EditorLayer)->Unit) {
    val original=layer.originalBitmap?:layer.bitmap
    var operations by remember(layer.id,layer.operations){mutableStateOf(layer.operations)}
    var editingBasicId by remember { mutableStateOf<String?>(null) }
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
                    if(op is EditOperation.BasicAdjust)TextButton(onClick={editingBasicId=op.id}){Text("Edit")}
                    TextButton(enabled=index>0,onClick={operations=operations.toMutableList().also{val item=it.removeAt(index);it.add(index-1,item)}}){Text("Up")}
                    TextButton(enabled=index<operations.lastIndex,onClick={operations=operations.toMutableList().also{val item=it.removeAt(index);it.add(index+1,item)}}){Text("Down")}
                    TextButton(onClick={operations=operations.filterIndexed{i,_->i!=index}}){Text("Remove")}
                }}
            }
        }
        if(operations.isEmpty())Text("No operations. Apply an adjustment to create a replayable operation.",color=Color.LightGray,modifier=Modifier.padding(12.dp))
    }
    editingBasicId?.let { id ->
        val current=operations.firstOrNull{it.id==id} as? EditOperation.BasicAdjust
        if(current==null) editingBasicId=null else BasicAdjustEditDialog(current,onDismiss={editingBasicId=null},onSave={values->
            operations=operations.map{if(it.id==id)current.copy(values=values) else it};editingBasicId=null
        })
    }
}
@Composable
private fun BasicAdjustEditDialog(operation:EditOperation.BasicAdjust,onDismiss:()->Unit,onSave:(BasicAdjustmentValues)->Unit){
    var values by remember(operation.id){mutableStateOf(operation.values)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Basic Adjust")},text={
        Column(Modifier.heightIn(max=480.dp).verticalScrollCompat(),verticalArrangement=Arrangement.spacedBy(4.dp)){
            BasicSlider("Brightness",values.brightness,-1f..1f){values=values.copy(brightness=it)}
            BasicSlider("Contrast",values.contrast,-1f..1f){values=values.copy(contrast=it)}
            BasicSlider("Exposure",values.exposure,-5f..5f){values=values.copy(exposure=it)}
            BasicSlider("Saturation",values.saturation,-1f..1f){values=values.copy(saturation=it)}
            BasicSlider("Hue",values.hue,-1f..1f){values=values.copy(hue=it)}
            BasicSlider("Temperature",values.temperature,-1f..1f){values=values.copy(temperature=it)}
            BasicSlider("Tint",values.tint,-1f..1f){values=values.copy(tint=it)}
            BasicSlider("Highlights",values.highlights,-1f..1f){values=values.copy(highlights=it)}
            BasicSlider("Shadows",values.shadows,-1f..1f){values=values.copy(shadows=it)}
            BasicSlider("Whites",values.whites,-1f..1f){values=values.copy(whites=it)}
            BasicSlider("Blacks",values.blacks,-1f..1f){values=values.copy(blacks=it)}
            BasicSlider("Vibrance",values.vibrance,-1f..1f){values=values.copy(vibrance=it)}
            BasicSlider("Clarity",values.clarity,-1f..1f){values=values.copy(clarity=it)}
            BasicSlider("Sharpen",values.sharpen,0f..1f){values=values.copy(sharpen=it)}
        }
    },confirmButton={Button(onClick={onSave(values)}){Text("Update operation")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable
private fun BasicSlider(label:String,value:Float,range:ClosedFloatingPointRange<Float>,onChange:(Float)->Unit){
    Column {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,color=Color.White,modifier=Modifier.weight(1f));Text("%.2f".format(value),color=Color.LightGray)}
        Slider(value=value.coerceIn(range.start,range.endInclusive),onValueChange=onChange,valueRange=range)
    }
}
@Composable
private fun ColumnScope.verticalScrollCompat():Modifier = androidx.compose.foundation.rememberScrollState().let { state -> Modifier.fillMaxWidth().then(androidx.compose.foundation.verticalScroll(state)) }

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
