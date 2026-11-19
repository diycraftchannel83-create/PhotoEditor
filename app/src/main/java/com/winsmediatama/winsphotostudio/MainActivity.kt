package com.winsmediatama.winsphotostudio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.ui.tools.CropTransformScreen
import com.winsmediatama.winsphotostudio.ui.tools.AdjustmentScreen
import com.winsmediatama.winsphotostudio.ui.tools.LevelsScreen
import com.winsmediatama.winsphotostudio.ui.tools.CurvesScreen
import com.winsmediatama.winsphotostudio.ui.tools.LayersScreen
import com.winsmediatama.winsphotostudio.ui.tools.ProjectScreen
import com.winsmediatama.winsphotostudio.ui.tools.BrushScreen
import com.winsmediatama.winsphotostudio.ui.tools.SelectionScreen
import com.winsmediatama.winsphotostudio.ui.tools.OperationStackScreen
import com.winsmediatama.winsphotostudio.ui.tools.TextShapeScreen
import com.winsmediatama.winsphotostudio.ui.tools.FilterScreen
import com.winsmediatama.winsphotostudio.ui.tools.EffectsScreen
import com.winsmediatama.winsphotostudio.ui.tools.LutScreen
import com.winsmediatama.winsphotostudio.ui.tools.CanvasSizeScreen
import com.winsmediatama.winsphotostudio.ui.tools.LayerMaskPaintScreen
import com.winsmediatama.winsphotostudio.ui.tools.BackgroundReplaceScreen
import com.winsmediatama.winsphotostudio.ui.tools.PerspectiveScreen
import com.winsmediatama.winsphotostudio.ui.tools.HslScreen
import com.winsmediatama.winsphotostudio.ui.tools.GradientScreen
import com.winsmediatama.winsphotostudio.ui.tools.RetouchScreen
import com.winsmediatama.winsphotostudio.ui.tools.BackgroundRemoveScreen
import com.winsmediatama.winsphotostudio.ui.tools.AiBackgroundRemoveScreen
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack
import com.winsmediatama.winsphotostudio.core.editor.LayerCompositeRenderer
import com.winsmediatama.winsphotostudio.core.editor.AlphaLockProcessor
import com.winsmediatama.winsphotostudio.core.storage.RecoveryStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WinsPhotoApp() }
    }
}

private val StudioBackground = Color(0xFF101216)
private val StudioSurface = Color(0xFF1B1E24)
private val StudioAccent = Color(0xFF77D7C0)

private data class EditorSnapshot(val layers:List<EditorLayer>,val selectedLayerId:String?,val canvas:Bitmap,val label:String)
private const val MAX_HISTORY_STATES=8

@Composable
private fun WinsPhotoApp() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cropTransformOpen by remember { mutableStateOf(false) }
    var historyOpen by remember{mutableStateOf(false)}
    var operationsOpen by remember{mutableStateOf(false)}
    var adjustmentOpen by remember { mutableStateOf(false) }
    var levelsOpen by remember { mutableStateOf(false) }
    var curvesOpen by remember { mutableStateOf(false) }
    var layersOpen by remember { mutableStateOf(false) }
    var projectsOpen by remember { mutableStateOf(false) }
    var brushOpen by remember { mutableStateOf(false) }
    var selectionOpen by remember { mutableStateOf(false) }
    var textShapeOpen by remember { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }
    var effectsOpen by remember { mutableStateOf(false) }
    var lutOpen by remember { mutableStateOf(false) }
    var canvasSizeOpen by remember { mutableStateOf(false) }
    var maskPaintOpen by remember { mutableStateOf(false) }
    var backgroundReplaceOpen by remember { mutableStateOf(false) }
    var perspectiveOpen by remember { mutableStateOf(false) }
    var maskPaintLayerId by remember { mutableStateOf<String?>(null) }
    var hslOpen by remember { mutableStateOf(false) }
    var gradientOpen by remember { mutableStateOf(false) }
    var retouchOpen by remember { mutableStateOf(false) }
    var backgroundRemoveOpen by remember { mutableStateOf(false) }
    var aiBackgroundRemoveOpen by remember { mutableStateOf(false) }
    var layers by remember { mutableStateOf<List<EditorLayer>>(emptyList()) }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var exportDialogOpen by remember { mutableStateOf(false) }
    var exportQuality by remember { mutableFloatStateOf(92f)
    }
    var exportFormat by remember { mutableStateOf("JPEG") }
    var exportBackground by remember { mutableIntStateOf(android.graphics.Color.WHITE) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var editHistory by remember { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
    var redoHistory by remember { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
    fun recordUndo(canvas:Bitmap,label:String){editHistory=(editHistory+EditorSnapshot(layers.toList(),selectedLayerId,canvas,label)).takeLast(MAX_HISTORY_STATES);redoHistory=emptyList()}
    fun restoreSnapshot(snapshot:EditorSnapshot){layers=snapshot.layers;selectedLayerId=snapshot.selectedLayerId;bitmap=snapshot.canvas}
    var recoveryCandidate by remember { mutableStateOf<RecoveryStore.Recovered?>(null) }
    fun commitEdit(edited:Bitmap,previousComposite:Bitmap,flatten:Boolean=false) {
        val selected=layers.firstOrNull{it.id==selectedLayerId}
        if(!flatten && selected?.locked==true){exportStatus="Selected layer is locked. Unlock it in Layers before editing.";return}
        recordUndo(previousComposite,"Edit")
        if(!flatten && selected!=null) {
            val finalEdited=if(selected.alphaLocked)AlphaLockProcessor.apply(selected.bitmap,edited) else edited
            val updated=layers.map{if(it.id==selected.id)it.copy(bitmap=finalEdited,originalBitmap=null,operations=emptyList()) else it}
            scope.launch {
                val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,previousComposite.width,previousComposite.height)}
                layers=updated;selectedLayerId=selected.id;bitmap=composite
            }
        } else {
            val backup=EditorLayer(name="Previous composite (hidden)",bitmap=previousComposite,visible=false)
            val resultLayer=EditorLayer(name="Applied edit",bitmap=edited)
            layers=listOf(backup,resultLayer);selectedLayerId=resultLayer.id;bitmap=edited
        }
    }
        fun commitOperation(edited:Bitmap,previousComposite:Bitmap,operation:EditOperation,flatten:Boolean=false) {
        if(flatten){
            recordUndo(previousComposite,operation.label)
            val original=previousComposite.copy(Bitmap.Config.ARGB_8888,true)
            val stack=NonDestructiveEditStack(original).apply{append(operation)}
            edited.recycle()
            scope.launch{
                val rendered=withContext(Dispatchers.IO){stack.render()}
                val backup=EditorLayer(name="Previous composite (hidden)",bitmap=previousComposite,visible=false)
                val result=EditorLayer(name=operation.label,bitmap=rendered,originalBitmap=original,operations=stack.operations)
                val updated=listOf(backup,result)
                val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,rendered.width,rendered.height)}
                layers=updated;selectedLayerId=result.id;bitmap=composite
            }
            return
        }
        val selected=layers.firstOrNull{it.id==selectedLayerId}
        if(selected==null){commitEdit(edited,previousComposite);return}
        if(selected.locked){edited.recycle();exportStatus="Selected layer is locked. Unlock it before applying adjustments.";return}
        recordUndo(previousComposite,operation.label)
        val original=selected.originalBitmap?:selected.bitmap.copy(Bitmap.Config.ARGB_8888,true)
        val stack=NonDestructiveEditStack(original,selected.operations).apply{append(operation)}
        edited.recycle()
        scope.launch{
            val rendered=withContext(Dispatchers.IO){stack.render()}
            val updated=layers.map{if(it.id==selected.id)it.copy(bitmap=rendered,originalBitmap=original,operations=stack.operations)else it}
            val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,previousComposite.width,previousComposite.height)}
            layers=updated;selectedLayerId=selected.id;bitmap=composite
        }
    }
    val jpegExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        val currentBitmap=bitmap
        if(uri!=null && currentBitmap!=null) scope.launch {
            val ok=withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.use { stream ->
                val flattened=flattenForJpeg(currentBitmap,exportBackground)
                val compressed=flattened.compress(Bitmap.CompressFormat.JPEG,exportQuality.toInt(),stream)
                flattened.recycle()
                compressed
            } ?: false }.getOrDefault(false) }
            exportStatus=if(ok) "JPEG exported successfully" else "Export failed"
        }
    }
    val pngExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val currentBitmap=bitmap
        if(uri!=null && currentBitmap!=null) scope.launch {
            val ok=withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.use { currentBitmap.compress(Bitmap.CompressFormat.PNG,100,it) } ?: false }.getOrDefault(false) }
            exportStatus=if(ok) "PNG exported successfully" else "Export failed"
        }
    }
    val layerImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val canvasBitmap=bitmap
        if(uri!=null && canvasBitmap!=null) scope.launch {
            val decoded=withContext(Dispatchers.IO) {
                runCatching {
                    val bounds=Options().apply{inJustDecodeBounds=true}
                    context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
                    val sample=calculateSampleSize(bounds.outWidth,bounds.outHeight,2048)
                    val opts=Options().apply{inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888}
                    context.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,opts)}
                }.getOrNull()
            }
            if(decoded!=null) {
                val fitted=withContext(Dispatchers.IO){fitBitmapToCanvas(decoded,canvasBitmap.width,canvasBitmap.height)}
                decoded.recycle()
                recordUndo(canvasBitmap,"Add layer")
                val updated=layers+EditorLayer(name="Photo Layer ${layers.size+1}",bitmap=fitted)
                val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,canvasBitmap.width,canvasBitmap.height)}
                layers=updated;bitmap=composite
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { imageUri = uri; loading = true; error = null; editHistory=emptyList();redoHistory=emptyList() }
    }
    LaunchedEffect(Unit) {
        recoveryCandidate=withContext(Dispatchers.IO){RecoveryStore(context).recover()}
    }
    LaunchedEffect(bitmap,layers,selectedLayerId) {
        val snapshot=bitmap
        if(snapshot!=null && layers.isNotEmpty()) {
            withContext(Dispatchers.IO){runCatching{RecoveryStore(context).save(layers,selectedLayerId,snapshot.width,snapshot.height)}}
        }
    }
    LaunchedEffect(imageUri) {
        val selected = imageUri ?: return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(selected)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val sample = calculateSampleSize(bounds.outWidth, bounds.outHeight, 2048)
                val decodeOptions = Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
                context.contentResolver.openInputStream(selected)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            }.getOrNull()
        }
        loading = false
        bitmap?.let { loaded -> val bg=EditorLayer(name="Background",bitmap=loaded);layers=listOf(bg);selectedLayerId=bg.id }
        if (bitmap == null) error = "Unable to open image. Try JPG, PNG or WebP."
    }
    MaterialTheme(colorScheme = darkColorScheme(background = StudioBackground, surface = StudioSurface, primary = StudioAccent, onPrimary = Color(0xFF08231D), onBackground = Color.White, onSurface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = StudioBackground) {
            val current = bitmap
            val selectedBitmap = remember(current,layers,selectedLayerId) { current?.let { canvas -> layers.firstOrNull{it.id==selectedLayerId}?.bitmap ?: canvas } ?: Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888) }
            if (current == null) HomeScreen(loading, error) { imagePicker.launch(arrayOf("image/*")) }
            else if (aiBackgroundRemoveOpen) AiBackgroundRemoveScreen(
                source=selectedBitmap,
                onCancel={aiBackgroundRemoveOpen=false},
                onApply={edited -> commitEdit(edited,current);aiBackgroundRemoveOpen=false}
            )
            else if (backgroundRemoveOpen) BackgroundRemoveScreen(
                source=selectedBitmap,
                onCancel={backgroundRemoveOpen=false},
                onApply={edited -> commitEdit(edited,current);backgroundRemoveOpen=false}
            )
            else if (retouchOpen) RetouchScreen(
                source=selectedBitmap,
                onCancel={retouchOpen=false},
                onApply={edited,strokes,strength,offsetX,offsetY -> commitOperation(edited,current,EditOperation.Retouch(strokes=strokes,strength=strength,offsetX=offsetX,offsetY=offsetY));retouchOpen=false}
            )
            else if (gradientOpen) GradientScreen(
                canvasWidth=current.width,
                canvasHeight=current.height,
                onCancel={gradientOpen=false},
                onApply={gradientBitmap -> recordUndo(current,"Gradient layer");scope.launch {
                    val layer=EditorLayer(name="Gradient Layer",bitmap=gradientBitmap)
                    val updated=layers+layer
                    val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,current.width,current.height)}
                    layers=updated;selectedLayerId=layer.id;bitmap=composite;gradientOpen=false
                }}
            )
            else if (hslOpen) HslScreen(
                source=selectedBitmap,
                onCancel={hslOpen=false},
                onApply={edited,values -> commitOperation(edited,current,EditOperation.Hsl(values=values));hslOpen=false}
            )
            else if (perspectiveOpen) PerspectiveScreen(source=current,onCancel={perspectiveOpen=false},onApply={edited,quad->commitOperation(edited,current,EditOperation.Perspective(quad=quad,outputWidth=edited.width,outputHeight=edited.height),flatten=true);perspectiveOpen=false})
            else if (backgroundReplaceOpen) BackgroundReplaceScreen(source=current,onCancel={backgroundReplaceOpen=false},onApply={edited->commitEdit(edited,current,flatten=true);backgroundReplaceOpen=false})
            else if (maskPaintOpen) {
                val maskLayer=layers.firstOrNull{it.id==maskPaintLayerId}
                val activeMask=maskLayer?.mask
                if(activeMask!=null) LayerMaskPaintScreen(mask=activeMask,onCancel={maskPaintOpen=false},onApply={editedMask->
                    recordUndo(current,"Paint mask")
                    val updated=layers.map{if(it.id==maskPaintLayerId)it.copy(mask=editedMask) else it}
                    layers=updated;maskPaintOpen=false;scope.launch{bitmap=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,current.width,current.height)}}
                }) else { Text("Mask not available",color=Color.White);TextButton(onClick={maskPaintOpen=false}){Text("Back")} }
            }
            else if (canvasSizeOpen) CanvasSizeScreen(
                source=current,
                onCancel={canvasSizeOpen=false},
                onApply={edited -> commitEdit(edited,current,flatten=true);canvasSizeOpen=false}
            )
            else if (lutOpen) LutScreen(
                source=selectedBitmap,
                onCancel={lutOpen=false},
                onApply={edited,cubeText -> commitOperation(edited,current,EditOperation.Lut(cubeText=cubeText),flatten=true);lutOpen=false}
            )
            else if (effectsOpen) EffectsScreen(
                source=selectedBitmap,
                onCancel={effectsOpen=false},
                onApply={edited,settings -> commitOperation(edited,current,EditOperation.Effect(settings=settings),flatten=true);effectsOpen=false}
            )
            else if (filtersOpen) FilterScreen(
                source=selectedBitmap,
                onCancel={filtersOpen=false},
                onApply={edited,values -> commitOperation(edited,current,EditOperation.BasicAdjust(values=values),flatten=true);filtersOpen=false}
            )
            else if (textShapeOpen) TextShapeScreen(
                source=selectedBitmap,
                onCancel={textShapeOpen=false},
                onApply={edited -> commitEdit(edited,current);textShapeOpen=false}
            )
            else if (selectionOpen) SelectionScreen(
                source=current,
                onCancel={selectionOpen=false},
                onApply={result -> when(result.action){
                    com.winsmediatama.winsphotostudio.ui.tools.SelectionAction.COPY_LAYER->{recordUndo(current,"Copy selection");scope.launch{val layer=EditorLayer(name="Selection Layer ${layers.size+1}",bitmap=result.bitmap);val updated=layers+layer;val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,current.width,current.height)};layers=updated;selectedLayerId=layer.id;bitmap=composite;selectionOpen=false}}
                    com.winsmediatama.winsphotostudio.ui.tools.SelectionAction.MASK->{val selected=layers.firstOrNull{it.id==selectedLayerId};val mask=result.selectionMask;if(selected==null||mask==null){mask?.recycle();result.bitmap.recycle();exportStatus="Select a layer before applying a selection mask";selectionOpen=false}else{recordUndo(current,"Selection mask");val finalMask=selected.mask?.let{com.winsmediatama.winsphotostudio.core.editor.LayerMaskProcessor.combineMultiply(it,mask)}?:mask;if(finalMask!==mask)mask.recycle();result.bitmap.recycle();val updated=layers.map{if(it.id==selected.id)it.copy(mask=finalMask)else it};scope.launch{val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,current.width,current.height)};layers=updated;bitmap=composite;selectionOpen=false}}
                    }
                    com.winsmediatama.winsphotostudio.ui.tools.SelectionAction.CUT->{commitEdit(result.bitmap,current,flatten=true);selectionOpen=false}
                    com.winsmediatama.winsphotostudio.ui.tools.SelectionAction.CUT_TO_LAYER->{val cutBase=EditorLayer(name="Cut Base",bitmap=result.bitmap);val copied=result.copiedLayerBitmap;if(copied==null){result.bitmap.recycle();exportStatus="Cut-to-layer selection output missing";selectionOpen=false}else{recordUndo(current,"Cut to layer");scope.launch{val top=EditorLayer(name="Cut Selection",bitmap=copied);val updated=listOf(cutBase,top);val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,current.width,current.height)};layers=updated;selectedLayerId=top.id;bitmap=composite;selectionOpen=false}}}
                    com.winsmediatama.winsphotostudio.ui.tools.SelectionAction.CROP->{commitEdit(result.bitmap,current,flatten=true);selectionOpen=false}
                }}
            )
            else if (brushOpen) BrushScreen(
                source=selectedBitmap,
                onCancel={brushOpen=false},
                onApply={edited -> commitEdit(edited,current);brushOpen=false}
            )
            else if (projectsOpen) ProjectScreen(
                source=current,
                layers=layers,
                selectedLayerId=selectedLayerId,
                onCancel={projectsOpen=false},
                onOpen={loadedProject -> projectsOpen=false;scope.launch {
                    val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(loadedProject.layers,loadedProject.canvasWidth,loadedProject.canvasHeight)}
                    bitmap=composite;layers=loadedProject.layers;selectedLayerId=loadedProject.selectedLayerId ?: loadedProject.layers.lastOrNull()?.id;editHistory=emptyList();redoHistory=emptyList()
                }}
            )
            else if (layersOpen) LayersScreen(
                layers=layers,
                selectedLayerId=selectedLayerId,
                onCancel={layersOpen=false},
                onAddImage={layerImagePicker.launch(arrayOf("image/*"))},
                onEditMask={layerId,maskBitmap->maskPaintLayerId=layerId;maskPaintOpen=true},
                onApply={newLayers,newSelected -> recordUndo(current,"Layer stack");layersOpen=false;selectedLayerId=newSelected; scope.launch { val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(newLayers,current.width,current.height)}; layers=newLayers; bitmap=composite }}
            )
            else if (curvesOpen) CurvesScreen(
                source=selectedBitmap,
                onCancel={curvesOpen=false},
                onApply={edited,values -> commitOperation(edited,current,EditOperation.Curves(values=values));curvesOpen=false}
            )
            else if (levelsOpen) LevelsScreen(
                source=selectedBitmap,
                onCancel={levelsOpen=false},
                onApply={edited,values -> commitOperation(edited,current,EditOperation.Levels(values=values));levelsOpen=false}
            )
            else if (adjustmentOpen) AdjustmentScreen(
                source = selectedBitmap,
                onCancel = { adjustmentOpen = false },
                onApply = { edited,values -> commitOperation(edited,current,EditOperation.BasicAdjust(values=values));adjustmentOpen = false }
            )
            else if (cropTransformOpen) CropTransformScreen(
                source = current,
                onCancel = { cropTransformOpen = false },
                onApply = { edited,transform ->
                    commitOperation(edited,current,transform,flatten=true)
                    cropTransformOpen = false
                }
            )
            else if(operationsOpen) {val target=layers.firstOrNull{it.id==selectedLayerId};if(target!=null)OperationStackScreen(layer=target,onCancel={operationsOpen=false},onApply={updatedLayer->recordUndo(current,"Operation stack");scope.launch{val updated=layers.map{if(it.id==updatedLayer.id)updatedLayer else it};val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,current.width,current.height)};layers=updated;selectedLayerId=updatedLayer.id;bitmap=composite;operationsOpen=false}}) else Text("No selected layer",color=Color.White)}
            else if(historyOpen) HistoryPanel(history=editHistory,onCancel={historyOpen=false},onRestore={index->val target=editHistory[index];restoreSnapshot(target);editHistory=editHistory.take(index);redoHistory=emptyList();historyOpen=false})
            else EditorPreviewScreen(
                current,
                onBack = { bitmap = null; imageUri = null; editHistory = emptyList();redoHistory=emptyList();layers=emptyList();selectedLayerId=null;scope.launch(Dispatchers.IO){RecoveryStore(context).clear()} },
                onImportAnother = { imagePicker.launch(arrayOf("image/*")) },
                onCropTransform = { cropTransformOpen = true },
                onAdjustments = { adjustmentOpen = true },
                onLevels = { levelsOpen = true },
                onCurves = { curvesOpen = true },
                onLayers = { layersOpen = true },
                onProjects = { projectsOpen = true },
                onBrush = { brushOpen = true },
                onSelection = { selectionOpen = true },
                onTextShape = { textShapeOpen = true },
                onFilters = { filtersOpen = true },
                onEffects = { effectsOpen = true },
                onLut = { lutOpen = true },
                onCanvasSize = { canvasSizeOpen = true },
                onBackgroundReplace = { backgroundReplaceOpen = true },
                onPerspective = { perspectiveOpen = true },
                onHsl = { hslOpen = true },
                onGradient = { gradientOpen = true },
                onRetouch = { retouchOpen = true },
                onBackgroundRemove = { backgroundRemoveOpen = true },
                onAiBackgroundRemove = { aiBackgroundRemoveOpen = true },
                onExport = { exportDialogOpen=true },
                onHistory = { historyOpen=true },
                onOperations = { operationsOpen=true },
                onUndo = { if (editHistory.isNotEmpty() && bitmap!=null) { redoHistory=(redoHistory+EditorSnapshot(layers.toList(),selectedLayerId,bitmap!!,"Redo state")).takeLast(30);val previous=editHistory.last();restoreSnapshot(previous);editHistory=editHistory.dropLast(1) } },
                onRedo = { if (redoHistory.isNotEmpty() && bitmap!=null) { editHistory=(editHistory+EditorSnapshot(layers.toList(),selectedLayerId,bitmap!!,"Undo state")).takeLast(30);val next=redoHistory.last();restoreSnapshot(next);redoHistory=redoHistory.dropLast(1) } },
                canUndo = editHistory.isNotEmpty(), canRedo = redoHistory.isNotEmpty(), undoLabel=editHistory.lastOrNull()?.label, redoLabel=redoHistory.lastOrNull()?.label, canOperations=layers.firstOrNull{it.id==selectedLayerId}?.operations?.isNotEmpty()==true
            )
            if(exportDialogOpen) AlertDialog(
                onDismissRequest={exportDialogOpen=false},
                title={Text("Export image")},
                text={Column {
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("JPEG","PNG").forEach { fmt->FilterChip(selected=exportFormat==fmt,onClick={exportFormat=fmt},label={Text(fmt)}) } }
                    if(exportFormat=="JPEG") {
                        Text("Quality: ${exportQuality.toInt()}%"); Slider(value=exportQuality,onValueChange={exportQuality=it},valueRange=50f..100f)
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected=exportBackground==android.graphics.Color.WHITE,onClick={exportBackground=android.graphics.Color.WHITE},label={Text("White background")})
                            FilterChip(selected=exportBackground==android.graphics.Color.BLACK,onClick={exportBackground=android.graphics.Color.BLACK},label={Text("Black background")})
                        }
                    }
                    Text("Current canvas: ${bitmap?.width} × ${bitmap?.height}px",style=MaterialTheme.typography.bodySmall)
                }},
                confirmButton={TextButton(onClick={
                    exportDialogOpen=false
                    if(exportFormat=="PNG") pngExporter.launch("WinsPhotoStudio.png") else jpegExporter.launch("WinsPhotoStudio.jpg")
                }){Text("Export")}},
                dismissButton={TextButton(onClick={exportDialogOpen=false}){Text("Cancel")}}
            )
            recoveryCandidate?.let { recovered ->
                if(current==null) AlertDialog(
                    onDismissRequest={recoveryCandidate=null},
                    title={Text("Recover previous session?")},
                    text={Text("A previous editing session was autosaved. Restore ${recovered.layers.size} layer(s)?")},
                    confirmButton={TextButton(onClick={
                        recoveryCandidate=null
                        scope.launch {
                            val restored=recovered.layers
                            val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(restored,recovered.canvasWidth,recovered.canvasHeight)}
                            bitmap=composite;layers=restored;selectedLayerId=recovered.selectedLayerId ?: restored.lastOrNull()?.id;editHistory=emptyList();redoHistory=emptyList()
                        }
                    }){Text("Restore")}},
                    dismissButton={TextButton(onClick={recoveryCandidate=null;scope.launch(Dispatchers.IO){RecoveryStore(context).clear()}}){Text("Discard")}}
                )
            }
            exportStatus?.let { status-> AlertDialog(onDismissRequest={exportStatus=null},title={Text("Export")},text={Text(status)},confirmButton={TextButton(onClick={exportStatus=null}){Text("OK")}}) }
        }
    }
}

@Composable
private fun HistoryPanel(history:List<EditorSnapshot>,onCancel:()->Unit,onRestore:(Int)->Unit) {
    Column(Modifier.fillMaxSize().background(StudioBackground)) {
        Row(Modifier.fillMaxWidth().background(StudioSurface).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Edit History",Modifier.weight(1f),color=Color.White,fontWeight=FontWeight.Bold)
            TextButton(onClick=onCancel){Text("Back")}
        }
        Text("Select a previous editor state to restore. The current state is represented by the canvas outside this list.",color=Color.LightGray,modifier=Modifier.padding(12.dp))
        androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(history.size){index->
                val state=history[index]
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF20242A))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Image(state.canvas.asImageBitmap(),null,Modifier.size(64.dp),contentScale=ContentScale.Crop)
                        Column(Modifier.weight(1f).padding(start=10.dp)) {
                            Text(state.label,color=Color.White,fontWeight=FontWeight.SemiBold)
                            Text("${state.canvas.width} × ${state.canvas.height}px • ${state.layers.size} layers",color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
                        }
                        Button(onClick={onRestore(index)}){Text("Restore")}
                    }
                }
            }
        }
        if(history.isEmpty())Text("No history states yet.",color=Color.LightGray,modifier=Modifier.padding(16.dp))
    }
}

@Composable
private fun HomeScreen(loading: Boolean, error: String?, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("WINS", color = StudioAccent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Photo Studio", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Professional photo editing. Built for Android.", color = Color.LightGray)
        Spacer(Modifier.height(32.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = StudioSurface)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Photo, null, tint = StudioAccent, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text("Start editing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Import a photo to open the editor workspace.", color = Color.LightGray)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Import Photo") }
            }
        }
        if (loading) { Spacer(Modifier.height(16.dp)); CircularProgressIndicator(color = StudioAccent); Text("Loading image…", color = Color.LightGray) }
        error?.let { Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(32.dp))
        Text("Offline-first • Non-destructive workflow", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EditorPreviewScreen(bitmap: Bitmap, onBack: () -> Unit, onImportAnother: () -> Unit, onCropTransform: () -> Unit, onAdjustments: () -> Unit, onLevels: () -> Unit, onCurves: () -> Unit, onLayers: () -> Unit, onProjects: () -> Unit, onBrush: () -> Unit, onSelection: () -> Unit, onTextShape: () -> Unit, onFilters: () -> Unit, onEffects: () -> Unit, onLut: () -> Unit, onCanvasSize: () -> Unit, onBackgroundReplace: () -> Unit, onPerspective: () -> Unit, onHsl: () -> Unit, onGradient: () -> Unit, onRetouch: () -> Unit, onBackgroundRemove: () -> Unit, onAiBackgroundRemove: () -> Unit, onExport: () -> Unit, onHistory:()->Unit, onOperations:()->Unit, onUndo: () -> Unit, onRedo: () -> Unit, canUndo: Boolean, canRedo: Boolean, undoLabel:String?, redoLabel:String?, canOperations:Boolean) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(StudioSurface)) {
            Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,contentDescription="Back")}
                Column(Modifier.weight(1f)) {
                    Text("Editor Preview",fontWeight=FontWeight.SemiBold)
                    Text("${bitmap.width} × ${bitmap.height}px",style=MaterialTheme.typography.labelSmall,color=Color.LightGray)
                }
                TextButton(onClick={zoom=1f;panX=0f;panY=0f}){Text("Fit")}
                TextButton(onClick=onUndo,enabled=canUndo){Text(undoLabel?.let{"Undo: $it"}?:"Undo")}
                TextButton(onClick=onRedo,enabled=canRedo){Text(redoLabel?.let{"Redo: $it"}?:"Redo")}
                TextButton(onClick=onExport){Text("Export")}
                TextButton(onClick=onHistory){Text("History")}
                TextButton(onClick=onOperations,enabled=canOperations){Text("Ops")}
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=4.dp)) {
                TextButton(onClick=onCropTransform){Text("Crop")}
                TextButton(onClick=onAdjustments){Text("Adjust")}
                TextButton(onClick=onLevels){Text("Levels")}
                TextButton(onClick=onCurves){Text("Curves")}
                TextButton(onClick=onLayers){Text("Layers")}
                TextButton(onClick=onProjects){Text("Project")}
                TextButton(onClick=onBrush){Text("Brush")}
                TextButton(onClick=onSelection){Text("Select")}
                TextButton(onClick=onTextShape){Text("Text/Shape")}
                TextButton(onClick=onFilters){Text("Filters")}
                TextButton(onClick=onEffects){Text("Effects")}
                TextButton(onClick=onLut){Text("LUT")}
                TextButton(onClick=onCanvasSize){Text("Canvas")}
                TextButton(onClick=onBackgroundReplace){Text("Replace BG")}
                TextButton(onClick=onPerspective){Text("Perspective")}
                TextButton(onClick=onHsl){Text("HSL")}
                TextButton(onClick=onGradient){Text("Gradient")}
                TextButton(onClick=onRetouch){Text("Retouch")}
                TextButton(onClick=onBackgroundRemove){Text("Color Key")}
                TextButton(onClick=onAiBackgroundRemove){Text("AI Remove")}
                TextButton(onClick=onImportAnother){Text("Replace")}
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF25282D)).padding(12.dp).pointerInput(bitmap) {
            detectTransformGestures { _, pan, gestureZoom, _ ->
                zoom = (zoom * gestureZoom).coerceIn(1f, 8f)
                if (zoom > 1f) { panX += pan.x; panY += pan.y } else { panX = 0f; panY = 0f }
            }
        }, contentAlignment = Alignment.Center) {
            Image(bitmap.asImageBitmap(), contentDescription = "Imported photo", modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = panX; translationY = panY }, contentScale = ContentScale.Fit)
        }
        Column(Modifier.fillMaxWidth().background(StudioSurface).padding(16.dp)) {
            Text("WinsPhoto Studio • Editor", fontWeight = FontWeight.Bold, color = StudioAccent)
            Text("Crop, transform, adjustments and undo are available. Source image is retained separately from the edit result.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun flattenForJpeg(source:Bitmap,backgroundColor:Int):Bitmap {
    val output=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
    val canvas=Canvas(output);canvas.drawColor(backgroundColor);canvas.drawBitmap(source,0f,0f,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG));return output
}

private fun fitBitmapToCanvas(source:Bitmap,width:Int,height:Int):Bitmap {
    val output=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
    val canvas=Canvas(output)
    val scale=minOf(width.toFloat()/source.width,height.toFloat()/source.height)
    val drawW=(source.width*scale).toInt();val drawH=(source.height*scale).toInt()
    val left=(width-drawW)/2;val top=(height-drawH)/2
    canvas.drawBitmap(source,null,Rect(left,top,left+drawW,top+drawH),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    return output
}

private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sample = 1
    while (width / sample > maxDimension || height / sample > maxDimension) sample *= 2
    return sample
}
