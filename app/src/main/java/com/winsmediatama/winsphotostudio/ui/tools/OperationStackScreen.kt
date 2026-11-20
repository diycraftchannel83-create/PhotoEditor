package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.winsmediatama.winsphotostudio.core.adjustments.LevelsChannel
import com.winsmediatama.winsphotostudio.core.adjustments.CurvePoint
import com.winsmediatama.winsphotostudio.core.adjustments.HslAdjustments
import com.winsmediatama.winsphotostudio.core.adjustments.HslColorRange
import com.winsmediatama.winsphotostudio.core.adjustments.HslRangeAdjustment
import com.winsmediatama.winsphotostudio.core.adjustments.PhotoEffectKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OperationStackScreen(layer:EditorLayer,onCancel:()->Unit,onApply:(EditorLayer)->Unit) {
    val original=layer.originalBitmap?:layer.bitmap
    var operations by remember(layer.id,layer.operations){mutableStateOf(layer.operations)}
    var editingBasicId by remember { mutableStateOf<String?>(null) }
    var editingOperationId by remember { mutableStateOf<String?>(null) }
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
                    if(op is EditOperation.Levels || op is EditOperation.Curves || op is EditOperation.Hsl || op is EditOperation.Effect)TextButton(onClick={editingOperationId=op.id}){Text("Edit")}
                    TextButton(enabled=index>0,onClick={operations=operations.toMutableList().also{val item=it.removeAt(index);it.add(index-1,item)}}){Text("Up")}
                    TextButton(enabled=index<operations.lastIndex,onClick={operations=operations.toMutableList().also{val item=it.removeAt(index);it.add(index+1,item)}}){Text("Down")}
                    TextButton(onClick={operations=operations.filterIndexed{i,_->i!=index}}){Text("Remove")}
                }}
            }
        }
        if(operations.isEmpty())Text("No operations. Apply an adjustment to create a replayable operation.",color=Color.LightGray,modifier=Modifier.padding(12.dp))
    }
    editingOperationId?.let { id ->
        val op=operations.firstOrNull{it.id==id}
        if(op==null)editingOperationId=null else OperationParameterDialog(op,onDismiss={editingOperationId=null},onSave={updated->operations=operations.map{if(it.id==id)updated else it};editingOperationId=null})
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
        Column(Modifier.heightIn(max=480.dp).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)){
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
private fun OperationParameterDialog(operation:EditOperation,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
    when(operation){
        is EditOperation.Levels -> LevelsEditDialog(operation,onDismiss,onSave)
        is EditOperation.Curves -> CurvesEditDialog(operation,onDismiss,onSave)
        is EditOperation.Hsl -> HslEditDialog(operation,onDismiss,onSave)
        is EditOperation.Effect -> EffectEditDialog(operation,onDismiss,onSave)
        else -> Unit
    }
}
private enum class EditLevelsChannel(val title:String){RGB("RGB"),RED("Red"),GREEN("Green"),BLUE("Blue")}
@Composable
private fun LevelsEditDialog(operation:EditOperation.Levels,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
    var values by remember(operation.id){mutableStateOf(operation.values)}
    var target by remember{mutableStateOf(EditLevelsChannel.RGB)}
    val channel=when(target){EditLevelsChannel.RGB->values.composite;EditLevelsChannel.RED->values.red;EditLevelsChannel.GREEN->values.green;EditLevelsChannel.BLUE->values.blue}
    fun update(v:LevelsChannel){values=when(target){EditLevelsChannel.RGB->values.copy(composite=v);EditLevelsChannel.RED->values.copy(red=v);EditLevelsChannel.GREEN->values.copy(green=v);EditLevelsChannel.BLUE->values.copy(blue=v)}}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Levels")},text={Column(Modifier.heightIn(max=470.dp).verticalScroll(rememberScrollState())){
        Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){EditLevelsChannel.entries.forEach{FilterChip(selected=target==it,onClick={target=it},label={Text(it.title)})}}
        BasicSlider("Input Black",channel.inputBlack,0f..0.95f){update(channel.copy(inputBlack=it.coerceAtMost(channel.inputWhite-.01f)))}
        BasicSlider("Gamma",channel.inputGamma,.1f..3f){update(channel.copy(inputGamma=it))}
        BasicSlider("Input White",channel.inputWhite,.05f..1f){update(channel.copy(inputWhite=it.coerceAtLeast(channel.inputBlack+.01f)))}
        BasicSlider("Output Black",channel.outputBlack,0f..1f){update(channel.copy(outputBlack=it.coerceAtMost(channel.outputWhite)))}
        BasicSlider("Output White",channel.outputWhite,0f..1f){update(channel.copy(outputWhite=it.coerceAtLeast(channel.outputBlack)))}
    }},confirmButton={Button(onClick={onSave(operation.copy(values=values))}){Text("Update")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
private fun curveIdentity()=listOf(CurvePoint(0f,0f),CurvePoint(.25f,.25f),CurvePoint(.5f,.5f),CurvePoint(.75f,.75f),CurvePoint(1f,1f))
private enum class EditCurveChannel(val title:String){RGB("RGB"),RED("Red"),GREEN("Green"),BLUE("Blue")}
@Composable
private fun CurvesEditDialog(operation:EditOperation.Curves,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
    var values by remember(operation.id){mutableStateOf(operation.values)}
    var target by remember{mutableStateOf(EditCurveChannel.RGB)}
    val curve=when(target){EditCurveChannel.RGB->values.composite;EditCurveChannel.RED->values.red.ifEmpty{curveIdentity()};EditCurveChannel.GREEN->values.green.ifEmpty{curveIdentity()};EditCurveChannel.BLUE->values.blue.ifEmpty{curveIdentity()}}
    fun setCurve(list:List<CurvePoint>){values=when(target){EditCurveChannel.RGB->values.copy(composite=list);EditCurveChannel.RED->values.copy(red=list);EditCurveChannel.GREEN->values.copy(green=list);EditCurveChannel.BLUE->values.copy(blue=list)}}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Curves")},text={Column(Modifier.heightIn(max=470.dp).verticalScroll(rememberScrollState())){
        Row(horizontalArrangement=Arrangement.spacedBy(3.dp)){EditCurveChannel.entries.forEach{FilterChip(selected=target==it,onClick={target=it},label={Text(it.title)})}}
        Text("Adjust control-point output values",color=Color.LightGray)
        curve.forEachIndexed{index,point->BasicSlider("Point "+(index+1)+" · "+(point.x*100).toInt()+"% input",point.y,0f..1f){v->setCurve(curve.toMutableList().also{it[index]=point.copy(y=v)})}}
        TextButton(onClick={setCurve(curveIdentity())}){Text("Reset selected channel")}
    }},confirmButton={Button(onClick={onSave(operation.copy(values=values))}){Text("Update")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable
private fun HslEditDialog(operation:EditOperation.Hsl,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
    var values by remember(operation.id){mutableStateOf(operation.values)}
    var target by remember{mutableStateOf(HslColorRange.RED)}
    val current=values.ranges[target]?:HslRangeAdjustment()
    fun update(v:HslRangeAdjustment){values=HslAdjustments(values.ranges.toMutableMap().also{it[target]=v})}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Edit HSL / Color Range")},text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState())){
        HslColorRange.entries.forEach{range->FilterChip(selected=target==range,onClick={target=range},label={Text(range.label)})}
        BasicSlider("Hue shift",current.hueShift,-180f..180f){update(current.copy(hueShift=it))}
        BasicSlider("Saturation",current.saturation,-1f..1f){update(current.copy(saturation=it))}
        BasicSlider("Lightness",current.lightness,-1f..1f){update(current.copy(lightness=it))}
        TextButton(onClick={update(HslRangeAdjustment())}){Text("Reset selected range")}
    }},confirmButton={Button(onClick={onSave(operation.copy(values=values))}){Text("Update")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable
private fun EffectEditDialog(operation:EditOperation.Effect,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
    var settings by remember(operation.id){mutableStateOf(operation.settings)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Effect")},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())){
        PhotoEffectKind.entries.forEach{kind->FilterChip(selected=settings.kind==kind,onClick={settings=settings.copy(kind=kind)},label={Text(kind.name)})}
        BasicSlider("Strength",settings.strength,0f..1f){settings=settings.copy(strength=it)}
        if(settings.kind==PhotoEffectKind.MOTION)BasicSlider("Angle",settings.angleDegrees,0f..360f){settings=settings.copy(angleDegrees=it)}
    }},confirmButton={Button(onClick={onSave(operation.copy(settings=settings))}){Text("Update")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
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
