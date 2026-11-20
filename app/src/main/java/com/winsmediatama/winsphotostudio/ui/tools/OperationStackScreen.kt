package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import com.winsmediatama.winsphotostudio.core.tools.CanvasAnchor
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun OperationStackScreen(layer:EditorLayer,onCancel:()->Unit,onApply:(EditorLayer)->Unit) {
    val original=layer.originalBitmap?:layer.bitmap
    var operations by remember(layer.id,layer.operations){mutableStateOf(layer.operations)}
    var editingBasicId by remember { mutableStateOf<String?>(null) }
    var undoOperations by remember(layer.id,layer.operations){mutableStateOf<List<List<EditOperation>>>(emptyList())}
    var redoOperations by remember(layer.id,layer.operations){mutableStateOf<List<List<EditOperation>>>(emptyList()}
    fun changeOperations(next:List<EditOperation>){undoOperations=(undoOperations+listOf(operations)).takeLast(8);redoOperations=emptyList();operations=next}
    var editingOperationId by remember { mutableStateOf<String?>(null) }
    val preview by produceState<Bitmap?>(null,original,operations){
        value?.takeIf{it!==original}?.recycle();value=null
        value=withContext(Dispatchers.IO){NonDestructiveEditStack(original,operations).render()}
    }
    androidx.compose.runtime.DisposableEffect(preview){onDispose{preview?.takeIf{it!==original}?.recycle()}}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))){
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp),verticalAlignment=Alignment.CenterVertically){Text("Non-destructive stack",Modifier.weight(1f),color=Color.White);TextButton(enabled=undoOperations.isNotEmpty(),onClick={val previous=undoOperations.last();redoOperations=(redoOperations+listOf(operations)).takeLast(8);undoOperations=undoOperations.dropLast(1);operations=previous}){Text("Undo")};TextButton(enabled=redoOperations.isNotEmpty(),onClick={val next=redoOperations.last();undoOperations=(undoOperations+listOf(operations)).takeLast(8);redoOperations=redoOperations.dropLast(1);operations=next}){Text("Redo")};TextButton(onClick=onCancel){Text("Cancel")};Button(enabled=preview!=null,onClick={preview?.let{onApply(layer.copy(bitmap=it.copy(Bitmap.Config.ARGB_8888,true),originalBitmap=original,operations=operations))}}){Text("Apply")}}
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center){preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit)}?:CircularProgressIndicator()}
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Text("Original source retained",color=Color.LightGray,modifier=Modifier.weight(1f));TextButton(onClick={changeOperations(emptyList())}){Text("Reset")}}
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            itemsIndexed(operations,key={_,op->op.id}){index,op->
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF20242A))){Row(Modifier.fillMaxWidth().padding(8.dp).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
                    Checkbox(checked=op.enabled,onCheckedChange={v->changeOperations(operations.toMutableList().also{it[index]=toggle(it[index],v)})})
                    Text(op.label,color=Color.White)
                    if(op is EditOperation.BasicAdjust)TextButton(onClick={editingBasicId=op.id}){Text("Edit")}
                    if(op !is EditOperation.BasicAdjust && op !is EditOperation.SelectionMask)TextButton(onClick={editingOperationId=op.id}){Text("Edit")}
                    TextButton(enabled=index>0,onClick={changeOperations(operations.toMutableList().also{val item=it.removeAt(index);it.add(index-1,item)})}){Text("Up")}
                    TextButton(enabled=index<operations.lastIndex,onClick={changeOperations(operations.toMutableList().also{val item=it.removeAt(index);it.add(index+1,item)})}){Text("Down")}
                    TextButton(onClick={changeOperations(operations.filterIndexed{i,_->i!=index})}){Text("Remove")}
                }}
            }
        }
        if(operations.isEmpty())Text("No operations. Apply an adjustment to create a replayable operation.",color=Color.LightGray,modifier=Modifier.padding(12.dp))
    }
    editingOperationId?.let { id ->
        val op=operations.firstOrNull{it.id==id}
        if(op==null)editingOperationId=null else OperationParameterDialog(op,onDismiss={editingOperationId=null},onSave={updated->changeOperations(operations.map{if(it.id==id)updated else it});editingOperationId=null})
    }
    editingBasicId?.let { id ->
        val current=operations.firstOrNull{it.id==id} as? EditOperation.BasicAdjust
        if(current==null) editingBasicId=null else BasicAdjustEditDialog(current,onDismiss={editingBasicId=null},onSave={values->
            changeOperations(operations.map{if(it.id==id)current.copy(values=values) else it});editingBasicId=null
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
        is EditOperation.Crop -> CropEditDialog(operation,onDismiss,onSave)
        is EditOperation.Transform -> TransformEditDialog(operation,onDismiss,onSave)
        is EditOperation.CanvasSize -> CanvasSizeEditDialog(operation,onDismiss,onSave)
        is EditOperation.Perspective -> PerspectiveEditDialog(operation,onDismiss,onSave)
        is EditOperation.BackgroundReplace -> BackgroundReplaceEditDialog(operation,onDismiss,onSave)
        is EditOperation.Retouch -> RetouchEditDialog(operation,onDismiss,onSave)
        is EditOperation.Lut -> LutEditDialog(operation,onDismiss,onSave)
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


@Composable
private fun CropEditDialog(operation:EditOperation.Crop,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
 var l by remember(operation.id){mutableFloatStateOf(operation.left)};var t by remember(operation.id){mutableFloatStateOf(operation.top)}
 var r by remember(operation.id){mutableFloatStateOf(operation.right)};var b by remember(operation.id){mutableFloatStateOf(operation.bottom)}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Crop")},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())){
 BasicSlider("Left",l,0f..(r-.01f).coerceAtLeast(0f)){l=it};BasicSlider("Top",t,0f..(b-.01f).coerceAtLeast(0f)){t=it}
 BasicSlider("Right",r,(l+.01f).coerceAtMost(.99f)..1f){r=it};BasicSlider("Bottom",b,(t+.01f).coerceAtMost(.99f)..1f){b=it}
 }},confirmButton={Button(onClick={onSave(operation.copy(left=l,top=t,right=r,bottom=b))}){Text("Update")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable
private fun TransformEditDialog(operation:EditOperation.Transform,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
 var l by remember(operation.id){mutableFloatStateOf(operation.left)};var t by remember(operation.id){mutableFloatStateOf(operation.top)}
 var r by remember(operation.id){mutableFloatStateOf(operation.right)};var b by remember(operation.id){mutableFloatStateOf(operation.bottom)}
 var rotation by remember(operation.id){mutableFloatStateOf(operation.rotationDegrees)}
 var fh by remember(operation.id){mutableStateOf(operation.flipHorizontal)};var fv by remember(operation.id){mutableStateOf(operation.flipVertical)}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Crop / Transform")},text={Column(Modifier.heightIn(max=470.dp).verticalScroll(rememberScrollState())){
 BasicSlider("Left",l,0f..(r-.01f).coerceAtLeast(0f)){l=it};BasicSlider("Top",t,0f..(b-.01f).coerceAtLeast(0f)){t=it}
 BasicSlider("Right",r,(l+.01f).coerceAtMost(.99f)..1f){r=it};BasicSlider("Bottom",b,(t+.01f).coerceAtMost(.99f)..1f){b=it}
 BasicSlider("Rotation",rotation,-180f..180f){rotation=it}
 Row(verticalAlignment=Alignment.CenterVertically){Checkbox(fh,{fh=it});Text("Flip horizontal",color=Color.White)}
 Row(verticalAlignment=Alignment.CenterVertically){Checkbox(fv,{fv=it});Text("Flip vertical",color=Color.White)}
 }},confirmButton={Button(onClick={onSave(operation.copy(left=l,top=t,right=r,bottom=b,rotationDegrees=rotation,flipHorizontal=fh,flipVertical=fv))}){Text("Update")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable
private fun CanvasSizeEditDialog(operation:EditOperation.CanvasSize,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
 var w by remember(operation.id){mutableFloatStateOf(operation.width.toFloat())};var h by remember(operation.id){mutableFloatStateOf(operation.height.toFloat())};var anchor by remember(operation.id){mutableStateOf(operation.anchor)}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Canvas Size")},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState())){
 BasicSlider("Width",w,1f..16384f){w=it};BasicSlider("Height",h,1f..16384f){h=it}
 Text("Anchor",color=Color.White);CanvasAnchor.entries.forEach{a->FilterChip(selected=anchor==a,onClick={anchor=a},label={Text(a.name.replace("_"," "))})}
 Text("Background color retained: "+operation.backgroundColor,color=Color.LightGray)
 }},confirmButton={Button(onClick={onSave(operation.copy(width=w.toInt().coerceAtLeast(1),height=h.toInt().coerceAtLeast(1),anchor=anchor))}){Text("Update")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable
private fun PerspectiveEditDialog(operation:EditOperation.Perspective,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
 var q by remember(operation.id){mutableStateOf(operation.quad)};var ow by remember(operation.id){mutableFloatStateOf(operation.outputWidth.toFloat())};var oh by remember(operation.id){mutableFloatStateOf(operation.outputHeight.toFloat())}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Perspective")},text={Column(Modifier.heightIn(max=470.dp).verticalScroll(rememberScrollState())){
 BasicSlider("Top-left X",q.topLeft.first,0f..1f){q=q.copy(topLeft=it to q.topLeft.second)};BasicSlider("Top-left Y",q.topLeft.second,0f..1f){q=q.copy(topLeft=q.topLeft.first to it)}
 BasicSlider("Top-right X",q.topRight.first,0f..1f){q=q.copy(topRight=it to q.topRight.second)};BasicSlider("Top-right Y",q.topRight.second,0f..1f){q=q.copy(topRight=q.topRight.first to it)}
 BasicSlider("Bottom-right X",q.bottomRight.first,0f..1f){q=q.copy(bottomRight=it to q.bottomRight.second)};BasicSlider("Bottom-right Y",q.bottomRight.second,0f..1f){q=q.copy(bottomRight=q.bottomRight.first to it)}
 BasicSlider("Bottom-left X",q.bottomLeft.first,0f..1f){q=q.copy(bottomLeft=it to q.bottomLeft.second)};BasicSlider("Bottom-left Y",q.bottomLeft.second,0f..1f){q=q.copy(bottomLeft=q.bottomLeft.first to it)}
 BasicSlider("Output width (0=source)",ow,0f..8192f){ow=it};BasicSlider("Output height (0=source)",oh,0f..8192f){oh=it}
 }},confirmButton={Button(onClick={onSave(operation.copy(quad=q,outputWidth=ow.toInt(),outputHeight=oh.toInt()))}){Text("Update")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable
private fun BackgroundReplaceEditDialog(operation:EditOperation.BackgroundReplace,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
 var mode by remember(operation.id){mutableStateOf(operation.mode)};var start by remember(operation.id){mutableStateOf(Color(operation.startColor))};var end by remember(operation.id){mutableStateOf(Color(operation.endColor))}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Background Replace")},text={Column(Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState())){
 listOf("solid","gradient","photo").forEach{FilterChip(selected=mode==it,onClick={mode=it},label={Text(it.uppercase())})}
 Text("Stored background colors retained; photo background remains unchanged.",color=Color.LightGray)
 BasicSlider("Start color alpha",start.alpha,0f..1f){start=start.copy(alpha=it)};BasicSlider("End color alpha",end.alpha,0f..1f){end=end.copy(alpha=it)}
 }},confirmButton={Button(onClick={onSave(operation.copy(mode=mode,startColor=start.toArgb(),endColor=end.toArgb()))}){Text("Update")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable
private fun RetouchEditDialog(operation:EditOperation.Retouch,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
 var strength by remember(operation.id){mutableFloatStateOf(operation.strength)};var x by remember(operation.id){mutableFloatStateOf(operation.offsetX)};var y by remember(operation.id){mutableFloatStateOf(operation.offsetY)}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Edit Retouch")},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())){
 Text(operation.strokes.size.toString()+" saved strokes retained",color=Color.LightGray);BasicSlider("Strength",strength,0f..1f){strength=it}
 BasicSlider("Offset X",x,-10000f..10000f){x=it};BasicSlider("Offset Y",y,-10000f..10000f){y=it}
 }},confirmButton={Button(onClick={onSave(operation.copy(strength=strength,offsetX=x,offsetY=y))}){Text("Update")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable
private fun LutEditDialog(operation:EditOperation.Lut,onDismiss:()->Unit,onSave:(EditOperation)->Unit){
 var cube by remember(operation.id){mutableStateOf(operation.cubeText)}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Edit LUT")},text={Column(Modifier.heightIn(max=420.dp)){
 Text("Retained .cube text (max 2 MB)",color=Color.LightGray);OutlinedTextField(value=cube,onValueChange={if(it.length<=2_000_000)cube=it},modifier=Modifier.fillMaxWidth().height(320.dp),maxLines=30)
 }},confirmButton={Button(enabled=runCatching{com.winsmediatama.winsphotostudio.core.adjustments.CubeLut.parse(cube)}.isSuccess,onClick={onSave(operation.copy(cubeText=cube))}){Text("Update")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
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
    is EditOperation.SelectionMask->op.copy(enabled=enabled)
}
