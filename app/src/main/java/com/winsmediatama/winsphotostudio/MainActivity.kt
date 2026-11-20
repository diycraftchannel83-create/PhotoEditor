package com.winsmediatama.winsphotostudio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
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
import com.winsmediatama.winsphotostudio.core.editor.EditorBlendMode
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack
import com.winsmediatama.winsphotostudio.core.adjustments.TransformProcessor
import com.winsmediatama.winsphotostudio.core.editor.LayerCompositeRenderer
import com.winsmediatama.winsphotostudio.core.editor.AlphaLockProcessor
import com.winsmediatama.winsphotostudio.core.storage.RecoveryStore
import com.winsmediatama.winsphotostudio.core.export.FullResolutionExport
import com.winsmediatama.winsphotostudio.core.export.NativeSourceLayerExport
import com.winsmediatama.winsphotostudio.core.render.TiledLayerCompositeRenderer
import com.winsmediatama.winsphotostudio.core.render.TiledPngEncoder
import com.winsmediatama.winsphotostudio.core.render.TiledOperationReplayProcessor
import com.winsmediatama.winsphotostudio.core.render.BitmapRenderScheduler
import com.winsmediatama.winsphotostudio.core.importer.PreviewImageDecoder
import com.winsmediatama.winsphotostudio.core.importer.OriginalSourceCache
import com.winsmediatama.winsphotostudio.core.importer.RegionPreviewAssembler
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
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

private enum class PendingEditorAction { BACK_TO_HOME, IMPORT_ANOTHER, OPEN_PROJECT }

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
    var exportOriginalDimensions by remember { mutableStateOf(false) }
    var sourceWidth by remember { mutableIntStateOf(0) }
    var sourceHeight by remember { mutableIntStateOf(0) }
    var originalSourceCached by remember { mutableStateOf(false) }
    var originalSourceBytes by remember { mutableLongStateOf(0L) }
    var sourceLayerId by remember { mutableStateOf<String?>(null) }
    var sourcePreviewWidth by remember { mutableIntStateOf(0) }
    var sourcePreviewHeight by remember { mutableIntStateOf(0) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var beforeAfterBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var beforeAfterLoading by remember { mutableStateOf(false) }
    var beforeAfterError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val compositeScheduler=remember{BitmapRenderScheduler(32L*1024L*1024L,"WinsPhoto-LayerComposite")}
    DisposableEffect(compositeScheduler){onDispose{compositeScheduler.close()}}
    suspend fun renderCompositeCached(renderLayers:List<EditorLayer>,width:Int,height:Int):Bitmap {
        val future=compositeScheduler.requestLayerStackComposite(renderLayers,width,height)
        return try { withContext(Dispatchers.IO){future.get()} }
        catch(error:Throwable){future.cancel(true);throw error}
    }
    LaunchedEffect(bitmap) { beforeAfterBitmap?.takeUnless{it.isRecycled}?.recycle();beforeAfterBitmap=null;beforeAfterLoading=false;beforeAfterError=null }
    val context = androidx.compose.ui.platform.LocalContext.current
    var editHistory by remember { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
    var redoHistory by remember { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
    var editorDirty by remember { mutableStateOf(false) }
    var pendingEditorAction by remember { mutableStateOf<PendingEditorAction?>(null) }
    var pendingProjectToOpen by remember { mutableStateOf<com.winsmediatama.winsphotostudio.core.storage.LoadedStudioProject?>(null) }
    fun prepareBeforeAfter() {
        if(beforeAfterLoading||beforeAfterBitmap!=null)return
        val current=bitmap?:return
        val sourceId=sourceLayerId?:return
        val sourceLayer=layers.firstOrNull{it.id==sourceId}?:return
        val original=sourceLayer.originalBitmap?:return
        val layerSnapshot=layers.toList()
        beforeAfterLoading=true
        beforeAfterError=null
        scope.launch {
            val baseline=runCatching { withContext(Dispatchers.IO) {
                val baselineLayers=layerSnapshot.map{layer->if(layer.id==sourceId)layer.copy(bitmap=original,originalBitmap=null,operations=emptyList(),mask=null,clipToBelow=false)else layer}
                renderCompositeCached(baselineLayers,current.width,current.height)
            } }.getOrNull()
            if(bitmap!==current||sourceLayerId!=sourceId) {
                baseline?.takeUnless{it.isRecycled}?.recycle()
                beforeAfterLoading=false
                return@launch
            }
            beforeAfterBitmap?.takeUnless{it.isRecycled}?.recycle()
            beforeAfterBitmap=baseline
            beforeAfterError=if(baseline==null)"Unable to render before preview. Try again." else null
            beforeAfterLoading=false
        }
    }
    fun operationStackHistoryLabel(before:List<EditOperation>,after:List<EditOperation>):String {
        if(before==after)return "Operation stack unchanged"
        val oldIds=before.map{it.id}.toSet();val newIds=after.map{it.id}.toSet()
        val added=after.filter{it.id !in oldIds};val removed=before.filter{it.id !in newIds}
        if(added.size==1&&removed.isEmpty())return "Add "+added.first().label
        if(removed.size==1&&added.isEmpty())return "Remove "+removed.first().label
        val changed=after.filter{a->before.firstOrNull{it.id==a.id}?.let{it!=a}==true}
        if(changed.size==1&&added.isEmpty()&&removed.isEmpty())return "Edit "+changed.first().label
        return "Edit operation stack ("+after.size+" operations)"
    }
    fun recordUndo(canvas:Bitmap,label:String){editHistory=(editHistory+EditorSnapshot(layers.toList(),selectedLayerId,canvas,label)).takeLast(MAX_HISTORY_STATES);redoHistory=emptyList();editorDirty=true}
    fun restoreSnapshot(snapshot:EditorSnapshot){layers=snapshot.layers;selectedLayerId=snapshot.selectedLayerId;bitmap=snapshot.canvas;editorDirty=true}
    var recoveryCandidate by remember { mutableStateOf<RecoveryStore.Recovered?>(null) }
    fun commitEdit(edited:Bitmap,previousComposite:Bitmap,flatten:Boolean=false) {
        val selected=layers.firstOrNull{it.id==selectedLayerId}
        if(!flatten && selected?.locked==true){exportStatus="Selected layer is locked. Unlock it in Layers before editing.";return}
        recordUndo(previousComposite,"Edit")
        if(!flatten && selected!=null) {
            val finalEdited=if(selected.alphaLocked)AlphaLockProcessor.apply(selected.bitmap,edited) else edited
            val updated=layers.map{if(it.id==selected.id)it.copy(bitmap=finalEdited,originalBitmap=null,operations=emptyList()) else it}
            scope.launch {
                val composite=withContext(Dispatchers.IO){renderCompositeCached(updated,previousComposite.width,previousComposite.height)}
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
                val composite=withContext(Dispatchers.IO){renderCompositeCached(updated,rendered.width,rendered.height)}
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
            val composite=withContext(Dispatchers.IO){renderCompositeCached(updated,previousComposite.width,previousComposite.height)}
            layers=updated;selectedLayerId=selected.id;bitmap=composite
        }
    }
    suspend fun buildExportBitmap(currentBitmap:Bitmap):Pair<Bitmap,Boolean> = withContext(Dispatchers.IO) {
        if(exportOriginalDimensions && originalSourceCached && sourceLayerId!=null && sourceWidth>0 && sourceHeight>0 &&
            sourceWidth.toLong()*sourceHeight<=FullResolutionExport.MAX_EXPORT_PIXELS) {
            runCatching {
                val cacheFile=File(context.filesDir,"original-source-cache/active-source.original")
                val cached=OriginalSourceCache.CachedSource(cacheFile,sourceWidth,sourceHeight,cacheFile.length())
                val source=OriginalSourceCache(File(context.filesDir,"original-source-cache")).decode(cached,FullResolutionExport.MAX_EXPORT_PIXELS)
                try {
                    NativeSourceLayerExport.render(source,layers,sourceLayerId!!,sourcePreviewWidth,sourcePreviewHeight)
                } finally { source.recycle() }
            }.getOrNull()?.let { return@withContext it to true }
        }
        val size=FullResolutionExport.targetSize(currentBitmap,sourceWidth,sourceHeight,exportOriginalDimensions)
        FullResolutionExport.render(currentBitmap,size) to false
    }
    val jpegExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        val currentBitmap=bitmap
        if(uri!=null && currentBitmap!=null) scope.launch {
            val exportCancellation=CancellationSignal()
            currentCoroutineContext()[Job]?.invokeOnCompletion { cause -> if(cause is CancellationException) exportCancellation.cancel() }
            var nativeReplayUsed=false
            val ok=withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.use { stream ->
                val (resized,nativeReplay)=buildExportBitmap(currentBitmap)
                nativeReplayUsed=nativeReplay
                try {
                    val flattened=flattenForJpeg(resized,exportBackground)
                    try { flattened.compress(Bitmap.CompressFormat.JPEG,exportQuality.toInt(),stream) }
                    finally { if(!flattened.isRecycled)flattened.recycle() }
                } finally {
                    if(resized!==currentBitmap&&!resized.isRecycled)resized.recycle()
                }
            } ?: false }.getOrDefault(false) }
            exportStatus=if(ok) (if(exportOriginalDimensions&&nativeReplayUsed) "JPEG exported using native source replay" else if(exportOriginalDimensions) "JPEG exported at source dimensions (preview resampled fallback)" else "JPEG exported successfully") else "Export failed"
        }
    }
    val pngExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val currentBitmap=bitmap
        val currentLayers=layers
        if(uri!=null && currentBitmap!=null) scope.launch {
            val exportCancellation=CancellationSignal()
            currentCoroutineContext()[Job]?.invokeOnCompletion { cause -> if(cause is CancellationException) exportCancellation.cancel() }
            var nativeReplayUsed=false
            var tiledExportUsed=false
            val ok=withContext(Dispatchers.IO) { val ioContext=currentCoroutineContext(); runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    if(!exportOriginalDimensions) {
                        val replayLayer=currentLayers.singleOrNull()
                        val replaySource=replayLayer?.originalBitmap?.takeUnless { it.isRecycled }
                        val canReplayTiles=replayLayer!=null && replaySource!=null &&
                            replayLayer.visible && replayLayer.opacity>=0.999f && replayLayer.mask==null &&
                            replayLayer.blendMode==EditorBlendMode.NORMAL && replayLayer.groupId==null && !replayLayer.clipToBelow &&
                            replaySource.width==currentBitmap.width && replaySource.height==currentBitmap.height &&
                            TiledOperationReplayProcessor.supportsHalo(replayLayer.operations) && replayLayer.operations.isNotEmpty()
                        val supportsStreaming=currentLayers.isNotEmpty() &&
                            currentLayers.none { it.groupId != null || it.clipToBelow || it.operations.isNotEmpty() } &&
                            currentLayers.all { it.bitmap.width == currentBitmap.width && it.bitmap.height == currentBitmap.height && (it.mask == null || (it.mask.width == currentBitmap.width && it.mask.height == currentBitmap.height)) }
                        val supportsGroupedReplay=currentLayers.any { it.groupId != null } && currentLayers.none { it.clipToBelow || it.blendMode == EditorBlendMode.DIFFERENCE || it.groupBlendMode == EditorBlendMode.DIFFERENCE } &&
                            currentLayers.all { it.bitmap.width == currentBitmap.width && it.bitmap.height == currentBitmap.height && (it.mask == null || (it.mask.width == currentBitmap.width && it.mask.height == currentBitmap.height)) } &&
                            currentLayers.filter { it.visible }.all { TiledOperationReplayProcessor.supportsHalo(it.operations) }
                        val supportsClippedReplay=currentLayers.any { it.clipToBelow } && currentLayers.none { it.groupId != null || it.blendMode == EditorBlendMode.DIFFERENCE } &&
                            currentLayers.all { it.bitmap.width == currentBitmap.width && it.bitmap.height == currentBitmap.height && (it.mask == null || (it.mask.width == currentBitmap.width && it.mask.height == currentBitmap.height)) } &&
                            currentLayers.filter { it.visible }.all { TiledOperationReplayProcessor.supportsHalo(it.operations) }
                        val supportsMultiLayerReplay=currentLayers.any { it.operations.isNotEmpty() } && currentLayers.none { it.groupId != null || it.clipToBelow } &&
                            currentLayers.all { it.bitmap.width == currentBitmap.width && it.bitmap.height == currentBitmap.height && (it.mask == null || (it.mask.width == currentBitmap.width && it.mask.height == currentBitmap.height)) } &&
                            currentLayers.filter { it.visible }.all { TiledOperationReplayProcessor.supportsHalo(it.operations) }
                        if(canReplayTiles) {
                            TiledPngEncoder.encodeOperationReplayStreaming(
                                requireNotNull(replaySource),replayLayer!!.operations,stream,tileSize=256,
                                cancellation=exportCancellation,cancelCheck={ ioContext.ensureActive() }
                            )
                            tiledExportUsed=true
                            true
                        } else if(supportsGroupedReplay) {
                            TiledLayerCompositeRenderer.encodeStreamingGroupedOperationLayers(
                                currentLayers,currentBitmap.width,currentBitmap.height,stream,tileSize=256,
                                cancellation=exportCancellation,cancelCheck={ ioContext.ensureActive() }
                            )
                            tiledExportUsed=true
                            true
                        } else if(supportsClippedReplay) {
                            TiledLayerCompositeRenderer.encodeStreamingClippedOperationLayers(
                                currentLayers,currentBitmap.width,currentBitmap.height,stream,tileSize=256,
                                cancellation=exportCancellation,cancelCheck={ ioContext.ensureActive() }
                            )
                            tiledExportUsed=true
                            true
                        } else if(supportsMultiLayerReplay) {
                            TiledLayerCompositeRenderer.encodeStreamingOperationLayers(
                                currentLayers,currentBitmap.width,currentBitmap.height,stream,tileSize=256,
                                cancellation=exportCancellation,cancelCheck={ ioContext.ensureActive() }
                            )
                            tiledExportUsed=true
                            true
                        } else if(supportsStreaming) {
                            TiledLayerCompositeRenderer.encodeStreamingPng(
                                currentLayers,currentBitmap.width,currentBitmap.height,stream,tileWidth=512,cancellation=exportCancellation,cancelCheck={ ioContext.ensureActive() }
                            )
                            tiledExportUsed=true
                            true
                        } else currentBitmap.compress(Bitmap.CompressFormat.PNG,100,stream)
                    } else {
                        val (resized,nativeReplay)=buildExportBitmap(currentBitmap)
                        nativeReplayUsed=nativeReplay
                        try { resized.compress(Bitmap.CompressFormat.PNG,100,stream) }
                        finally { if(resized!==currentBitmap&&!resized.isRecycled)resized.recycle() }
                    }
                } ?: false
            }.getOrDefault(false) }
            exportStatus=if(ok) (if(tiledExportUsed) "PNG exported using streaming tiled compositor" else if(exportOriginalDimensions&&nativeReplayUsed) "PNG exported using native source replay" else if(exportOriginalDimensions) "PNG exported at source dimensions (preview resampled fallback)" else "PNG exported successfully") else "Export failed"
        }
    }
    val layerImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val canvasBitmap=bitmap
        if(uri!=null && canvasBitmap!=null) scope.launch {
            val decoded=withContext(Dispatchers.IO) {
                runCatching { PreviewImageDecoder.decode(context.contentResolver, uri)?.bitmap }.getOrNull()
            }
            if(decoded!=null) {
                val fitted=withContext(Dispatchers.IO){fitBitmapToCanvas(decoded,canvasBitmap.width,canvasBitmap.height)}
                decoded.recycle()
                recordUndo(canvasBitmap,"Add layer")
                val updated=layers+EditorLayer(name="Photo Layer ${layers.size+1}",bitmap=fitted)
                val composite=withContext(Dispatchers.IO){renderCompositeCached(updated,canvasBitmap.width,canvasBitmap.height)}
                layers=updated;bitmap=composite
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { imageUri = uri; loading = true; error = null; editHistory=emptyList();redoHistory=emptyList();editorDirty=false }
    }
    fun executePendingEditorAction(action:PendingEditorAction?,project:com.winsmediatama.winsphotostudio.core.storage.LoadedStudioProject?=null){
        pendingEditorAction=null;pendingProjectToOpen=null
        when(action){
            PendingEditorAction.BACK_TO_HOME->{bitmap=null;imageUri=null;editHistory=emptyList();redoHistory=emptyList();layers=emptyList();selectedLayerId=null;editorDirty=false;scope.launch(Dispatchers.IO){RecoveryStore(context).clear()}}
            PendingEditorAction.IMPORT_ANOTHER->{imagePicker.launch(arrayOf("image/*"))}
            PendingEditorAction.OPEN_PROJECT->{if(project!=null)scope.launch{val composite=withContext(Dispatchers.IO){renderCompositeCached(project.layers,project.canvasWidth,project.canvasHeight)};bitmap=composite;layers=project.layers;selectedLayerId=project.selectedLayerId?:project.layers.lastOrNull()?.id;editHistory=emptyList();redoHistory=emptyList();editorDirty=false}}
            null->Unit
        }
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
        val importResult = withContext(Dispatchers.IO) {
            val cacheResult=runCatching {
                OriginalSourceCache(File(context.filesDir,"original-source-cache"))
                    .cache(context.contentResolver,selected,"active-source")
            }.getOrNull()
            val preview=runCatching { PreviewImageDecoder.decode(context.contentResolver,selected) }.getOrNull()
                ?: runCatching { RegionPreviewAssembler.decode(context.contentResolver,selected) }.getOrNull()
            Pair(cacheResult,preview)
        }
        val cachedSource=importResult.first
        val decodedInfo=importResult.second
        originalSourceCached=cachedSource!=null
        originalSourceBytes=cachedSource?.byteCount?:0L
        bitmap=decodedInfo?.bitmap
        editorDirty=false
        sourceWidth=cachedSource?.width?:decodedInfo?.sourceWidth?:0
        sourceHeight=cachedSource?.height?:decodedInfo?.sourceHeight?:0
        sourcePreviewWidth=decodedInfo?.bitmap?.width?:0
        sourcePreviewHeight=decodedInfo?.bitmap?.height?:0
        exportOriginalDimensions=false
        loading = false
        bitmap?.let { loaded -> val bg=EditorLayer(name="Background",bitmap=loaded,originalBitmap=loaded);layers=listOf(bg);selectedLayerId=bg.id;sourceLayerId=bg.id }
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
                    val composite=withContext(Dispatchers.IO){renderCompositeCached(updated,current.width,current.height)}
                    layers=updated;selectedLayerId=layer.id;bitmap=composite;gradientOpen=false
                }}
            )
            else if (hslOpen) HslScreen(
                source=selectedBitmap,
                onCancel={hslOpen=false},
                onApply={edited,values -> commitOperation(edited,current,EditOperation.Hsl(values=values));hslOpen=false}
            )
            else if (perspectiveOpen) PerspectiveScreen(source=current,onCancel={perspectiveOpen=false},onApply={edited,quad->commitOperation(edited,current,EditOperation.Perspective(quad=quad,outputWidth=edited.width,outputHeight=edited.height),flatten=true);perspectiveOpen=false})
            else if (backgroundReplaceOpen) BackgroundReplaceScreen(source=current,onCancel={backgroundReplaceOpen=false},onApply={edited,mode,startColor,endColor,photoData->commitOperation(edited,current,EditOperation.BackgroundReplace(mode=mode,startColor=startColor,endColor=endColor,photoPngBase64=photoData),flatten=true);backgroundReplaceOpen=false})
            else if (maskPaintOpen) {
                val maskLayer=layers.firstOrNull{it.id==maskPaintLayerId}
                val activeMask=maskLayer?.mask
                if(activeMask!=null) LayerMaskPaintScreen(mask=activeMask,onCancel={maskPaintOpen=false},onApply={editedMask->
                    recordUndo(current,"Paint mask")
                    val updated=layers.map{if(it.id==maskPaintLayerId)it.copy(mask=editedMask) else it}
                    layers=updated;maskPaintOpen=false;scope.launch{bitmap=withContext(Dispatchers.IO){renderCompositeCached(updated,current.width,current.height)}}
                }) else { Text("Mask not available",color=Color.White);TextButton(onClick={maskPaintOpen=false}){Text("Back")} }
            }
            else if (canvasSizeOpen) CanvasSizeScreen(
                source=current,
                onCancel={canvasSizeOpen=false},
                onApply={edited,width,height,anchor,bg -> commitOperation(edited,current,EditOperation.CanvasSize(width=width,height=height,anchor=anchor,backgroundColor=bg),flatten=true);canvasSizeOpen=false}
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
                source = current,
                onCancel = { selectionOpen = false },
                onApply = { result ->
                    val selected = layers.firstOrNull { it.id == selectedLayerId }
                    when (result.action) {
                        com.winsmediatama.winsphotostudio.ui.tools.SelectionAction.COPY_LAYER -> {
                            recordUndo(current, "Copy selection")
                            scope.launch {
                                val mask = result.selectionMask
                                val operation = mask?.let {
                                    runCatching { EditOperation.SelectionMask.fromBitmap(it) }.getOrNull()
                                }
                                val newLayer = if (operation != null) {
                                    EditorLayer(
                                        name = "Selection Layer ${layers.size + 1}",
                                        bitmap = result.bitmap,
                                        originalBitmap = current.copy(Bitmap.Config.ARGB_8888, true),
                                        operations = listOf(operation)
                                    )
                                } else EditorLayer(name = "Selection Layer ${layers.size + 1}", bitmap = result.bitmap)
                                mask?.takeUnless { it.isRecycled }?.recycle()
                                val updated = layers + newLayer
                                val composite = withContext(Dispatchers.IO) {
                                    renderCompositeCached(updated, current.width, current.height)
                                }
                                layers = updated; selectedLayerId = newLayer.id; bitmap = composite; selectionOpen = false
                            }
                        }
                        com.winsmediatama.winsphotostudio.ui.tools.SelectionAction.MASK -> {
                            val mask = result.selectionMask
                            if (selected == null || mask == null) {
                                mask?.takeUnless { it.isRecycled }?.recycle()
                                result.bitmap.takeUnless { it.isRecycled }?.recycle()
                                exportStatus = "Select a layer before applying a selection mask"
                                selectionOpen = false
                            } else {
                                val operation = runCatching { EditOperation.SelectionMask.fromBitmap(mask) }.getOrNull()
                                mask.takeUnless { it.isRecycled }?.recycle()
                                result.bitmap.takeUnless { it.isRecycled }?.recycle()
                                if (operation == null) {
                                    exportStatus = "Selection mask could not be stored"; selectionOpen = false
                                } else {
                                    recordUndo(current, "Selection mask")
                                    val original = selected.originalBitmap ?: selected.bitmap
                                    val updatedLayer = selected.copy(originalBitmap = original, operations = selected.operations + operation)
                                    scope.launch {
                                        val rendered = withContext(Dispatchers.IO) { NonDestructiveEditStack(original, updatedLayer.operations).render() }
                                        val updated = layers.map { if (it.id == selected.id) updatedLayer.copy(bitmap = rendered) else it }
                                        val composite = withContext(Dispatchers.IO) { renderCompositeCached(updated, current.width, current.height) }
                                        layers = updated; bitmap = composite; selectionOpen = false
                                    }
                                }
                            }
                        }
                        com.winsmediatama.winsphotostudio.ui.tools.SelectionAction.CUT -> {
                            val mask = result.selectionMask
                            if (selected == null || mask == null) {
                                mask?.takeUnless { it.isRecycled }?.recycle()
                                commitEdit(result.bitmap, current, flatten = true); selectionOpen = false
                            } else {
                                val operation = runCatching {
                                    EditOperation.SelectionMask.fromBitmap(mask).copy(invertSelection = true)
                                }.getOrNull()
                                mask.takeUnless { it.isRecycled }?.recycle()
                                if (operation == null) {
                                    exportStatus = "Could not create a non-destructive cut operation"
                                    commitEdit(result.bitmap, current, flatten = true)
                                } else {
                                    recordUndo(current, "Selection cut")
                                    result.bitmap.takeUnless { it.isRecycled }?.recycle()
                                    val original = selected.originalBitmap ?: selected.bitmap
                                    val updatedLayer = selected.copy(originalBitmap = original, operations = selected.operations + operation)
                                    scope.launch {
                                        val rendered = withContext(Dispatchers.IO) { NonDestructiveEditStack(original, updatedLayer.operations).render() }
                                        val updated = layers.map { if (it.id == selected.id) updatedLayer.copy(bitmap = rendered) else it }
                                        val composite = withContext(Dispatchers.IO) { renderCompositeCached(updated, current.width, current.height) }
                                        layers = updated; bitmap = composite
                                    }
                                }
                                selectionOpen = false
                            }
                        }
                        com.winsmediatama.winsphotostudio.ui.tools.SelectionAction.CUT_TO_LAYER -> {
                            val copied = result.copiedLayerBitmap
                            val mask = result.selectionMask
                            if (copied == null || mask == null) {
                                result.bitmap.takeUnless { it.isRecycled }?.recycle()
                                copied?.takeUnless { it.isRecycled }?.recycle()
                                mask?.takeUnless { it.isRecycled }?.recycle()
                                exportStatus = "Cut-to-layer selection output missing"; selectionOpen = false
                            } else {
                                recordUndo(current, "Cut to layer")
                                scope.launch {
                                    val baseOperation = runCatching {
                                        EditOperation.SelectionMask.fromBitmap(mask).copy(invertSelection = true)
                                    }.getOrNull()
                                    val topOperation = baseOperation?.copy(invertSelection = false)
                                    val baseLayer = if (baseOperation != null) EditorLayer(
                                        name = "Cut Base", bitmap = result.bitmap,
                                        originalBitmap = current.copy(Bitmap.Config.ARGB_8888, true), operations = listOf(baseOperation)
                                    ) else EditorLayer(name = "Cut Base", bitmap = result.bitmap)
                                    val topLayer = if (topOperation != null) EditorLayer(
                                        name = "Cut Selection", bitmap = copied,
                                        originalBitmap = current.copy(Bitmap.Config.ARGB_8888, true), operations = listOf(topOperation)
                                    ) else EditorLayer(name = "Cut Selection", bitmap = copied)
                                    mask.takeUnless { it.isRecycled }?.recycle()
                                    val updated = listOf(baseLayer, topLayer)
                                    val composite = withContext(Dispatchers.IO) { renderCompositeCached(updated, current.width, current.height) }
                                    layers = updated; selectedLayerId = topLayer.id; bitmap = composite; selectionOpen = false
                                }
                            }
                        }
                        com.winsmediatama.winsphotostudio.ui.tools.SelectionAction.CROP -> {
                            val mask = result.selectionMask
                            if (mask != null && !mask.isRecycled) {
                                val pixels = IntArray(mask.width * mask.height)
                                mask.getPixels(pixels, 0, mask.width, 0, 0, mask.width, mask.height)
                                var minX = mask.width; var minY = mask.height; var maxX = -1; var maxY = -1
                                pixels.forEachIndexed { index, color ->
                                    if (android.graphics.Color.red(color) > 0) {
                                        val x = index % mask.width; val y = index / mask.width
                                        minX = minOf(minX, x); minY = minOf(minY, y); maxX = maxOf(maxX, x); maxY = maxOf(maxY, y)
                                    }
                                }
                                if (maxX >= minX && maxY >= minY) {
                                    val transform = EditOperation.Transform(
                                        left = minX.toFloat() / mask.width, top = minY.toFloat() / mask.height,
                                        right = (maxX + 1).toFloat() / mask.width, bottom = (maxY + 1).toFloat() / mask.height
                                    )
                                    commitOperation(result.bitmap, current, transform, flatten = true)
                                } else commitEdit(result.bitmap, current, flatten = true)
                                mask.takeUnless { it.isRecycled }?.recycle()
                            } else commitEdit(result.bitmap, current, flatten = true)
                            selectionOpen = false
                        }
                    }
                }
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
                onSaved={editorDirty=false},
                onOpen={loadedProject -> projectsOpen=false;if(editorDirty){pendingEditorAction=PendingEditorAction.OPEN_PROJECT;pendingProjectToOpen=loadedProject}else executePendingEditorAction(PendingEditorAction.OPEN_PROJECT,loadedProject)}
            )
            else if (layersOpen) LayersScreen(
                layers=layers,
                selectedLayerId=selectedLayerId,
                onCancel={layersOpen=false},
                onAddImage={layerImagePicker.launch(arrayOf("image/*"))},
                onEditMask={layerId,maskBitmap->maskPaintLayerId=layerId;maskPaintOpen=true},
                onApply={newLayers,newSelected -> recordUndo(current,"Layer stack");layersOpen=false;selectedLayerId=newSelected; scope.launch { val composite=withContext(Dispatchers.IO){renderCompositeCached(newLayers,current.width,current.height)}; layers=newLayers; bitmap=composite }}
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
            else if(operationsOpen) {val target=layers.firstOrNull{it.id==selectedLayerId};if(target!=null)OperationStackScreen(layer=target,onCancel={operationsOpen=false},onApply={updatedLayer->recordUndo(current,operationStackHistoryLabel(target.operations,updatedLayer.operations));scope.launch{val updated=layers.map{if(it.id==updatedLayer.id)updatedLayer else it};val composite=withContext(Dispatchers.IO){renderCompositeCached(updated,current.width,current.height)};layers=updated;selectedLayerId=updatedLayer.id;bitmap=composite;operationsOpen=false}}) else Text("No selected layer",color=Color.White)}
            else if(historyOpen) HistoryPanel(history=editHistory,onCancel={historyOpen=false},onRestore={index->val target=editHistory[index];restoreSnapshot(target);editHistory=editHistory.take(index);redoHistory=emptyList();historyOpen=false})
            else EditorPreviewScreen(
                current,
                onBack = { if(editorDirty){pendingEditorAction=PendingEditorAction.BACK_TO_HOME}else executePendingEditorAction(PendingEditorAction.BACK_TO_HOME) },
                onImportAnother = { if(editorDirty){pendingEditorAction=PendingEditorAction.IMPORT_ANOTHER}else executePendingEditorAction(PendingEditorAction.IMPORT_ANOTHER) },
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
                onBeforeAfter = { prepareBeforeAfter() }, beforeAfterBitmap=beforeAfterBitmap, beforeAfterLoading=beforeAfterLoading, beforeAfterError=beforeAfterError, beforeAfterAvailable=layers.any{it.id==sourceLayerId&&it.originalBitmap!=null},
                onUndo = { if (editHistory.isNotEmpty() && bitmap!=null) { redoHistory=(redoHistory+EditorSnapshot(layers.toList(),selectedLayerId,bitmap!!,"Redo state")).takeLast(MAX_HISTORY_STATES);val previous=editHistory.last();restoreSnapshot(previous);editHistory=editHistory.dropLast(1) } },
                onRedo = { if (redoHistory.isNotEmpty() && bitmap!=null) { editHistory=(editHistory+EditorSnapshot(layers.toList(),selectedLayerId,bitmap!!,"Undo state")).takeLast(MAX_HISTORY_STATES);val next=redoHistory.last();restoreSnapshot(next);redoHistory=redoHistory.dropLast(1) } },
                canUndo = editHistory.isNotEmpty(), canRedo = redoHistory.isNotEmpty(), undoLabel=editHistory.lastOrNull()?.label, redoLabel=redoHistory.lastOrNull()?.label, canOperations=layers.firstOrNull{it.id==selectedLayerId}?.operations?.isNotEmpty()==true
            )
            if(pendingEditorAction!=null) AlertDialog(
                onDismissRequest={pendingEditorAction=null;pendingProjectToOpen=null},
                title={Text("Unsaved changes")},
                text={Text("Your current project has unsaved changes. Save opens the project manager so you can save first; after saving, repeat the action. Discard continues immediately.")},
                confirmButton={TextButton(onClick={pendingEditorAction=null;pendingProjectToOpen=null;projectsOpen=true}){Text("Save project")}},
                dismissButton={Row {
                    TextButton(onClick={val action=pendingEditorAction;val project=pendingProjectToOpen;executePendingEditorAction(action,project)}){Text("Discard")}
                    TextButton(onClick={pendingEditorAction=null;pendingProjectToOpen=null}){Text("Cancel")}
                }}
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
                    Text("Privacy: edited exports do not copy source EXIF or location metadata.",style=MaterialTheme.typography.labelSmall,color=Color.LightGray)
                    if(sourceWidth>0&&sourceHeight>0) {
                        Text(if(originalSourceCached) "Original source retained on disk (${originalSourceBytes/1024} KB)" else "Original source cache unavailable; preview remains editable",style=MaterialTheme.typography.labelSmall,color=if(originalSourceCached)Color(0xFF77D7C0) else Color(0xFFFFC107))
                    }
                    if(sourceWidth>0&&sourceHeight>0&&sourceWidth.toLong()*sourceHeight<=FullResolutionExport.MAX_EXPORT_PIXELS) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Checkbox(checked=exportOriginalDimensions,onCheckedChange={exportOriginalDimensions=it})
                            Column {
                                Text("Export at source dimensions")
                                Text("${sourceWidth} × ${sourceHeight} px (native replay when available; fallback resamples preview)",style=MaterialTheme.typography.labelSmall,color=Color.LightGray)
                            }
                        }
                    } else if(sourceWidth>0&&sourceHeight>0) {
                        Text("Source is larger than the 24 MP export safety limit. Export will use current preview size.",color=Color(0xFFFFC107),style=MaterialTheme.typography.bodySmall)
                    }
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
                            val composite=withContext(Dispatchers.IO){renderCompositeCached(restored,recovered.canvasWidth,recovered.canvasHeight)}
                            bitmap=composite;layers=restored;selectedLayerId=recovered.selectedLayerId ?: restored.lastOrNull()?.id;editHistory=emptyList();redoHistory=emptyList();editorDirty=true
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
private fun EditorPreviewScreen(bitmap: Bitmap, onBack: () -> Unit, onImportAnother: () -> Unit, onCropTransform: () -> Unit, onAdjustments: () -> Unit, onLevels: () -> Unit, onCurves: () -> Unit, onLayers: () -> Unit, onProjects: () -> Unit, onBrush: () -> Unit, onSelection: () -> Unit, onTextShape: () -> Unit, onFilters: () -> Unit, onEffects: () -> Unit, onLut: () -> Unit, onCanvasSize: () -> Unit, onBackgroundReplace: () -> Unit, onPerspective: () -> Unit, onHsl: () -> Unit, onGradient: () -> Unit, onRetouch: () -> Unit, onBackgroundRemove: () -> Unit, onAiBackgroundRemove: () -> Unit, onExport: () -> Unit, onHistory:()->Unit, onOperations:()->Unit, onBeforeAfter:()->Unit, beforeAfterBitmap:Bitmap?, beforeAfterLoading:Boolean, beforeAfterError:String?, beforeAfterAvailable:Boolean, onUndo: () -> Unit, onRedo: () -> Unit, canUndo: Boolean, canRedo: Boolean, undoLabel:String?, redoLabel:String?, canOperations:Boolean) {
    var beforeAfterVisible by remember(bitmap){mutableStateOf(false)}
    val displayedBitmap=if(beforeAfterVisible&&beforeAfterBitmap!=null)beforeAfterBitmap else bitmap
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var activeMenuCategory by remember { mutableStateOf("Canvas") }
    val menuCategories = listOf("Canvas","Color","Layers","Design","AI")
    Column(Modifier.fillMaxSize().background(StudioBackground)) {
        Column(Modifier.fillMaxWidth().background(Color(0xFF15181D)).padding(horizontal=12.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,contentDescription="Back",tint=Color.White)}
                Column(Modifier.weight(1f)) {
                    Text("WINS PHOTO STUDIO",fontWeight=FontWeight.Bold,letterSpacing=1.2f,color=StudioAccent,style=MaterialTheme.typography.titleSmall)
                    Text("Creative workspace  •  ${bitmap.width} × ${bitmap.height}px",style=MaterialTheme.typography.labelSmall,color=Color(0xFF9DA5B0))
                }
                TextButton(onClick={zoom=1f;panX=0f;panY=0f}){Text("FIT")}
                TextButton(onClick=onExport,enabled=!beforeAfterVisible){Text("EXPORT",fontWeight=FontWeight.Bold)}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically) {
                menuCategories.forEach { category ->
                    val selected=activeMenuCategory==category
                    Surface(onClick={activeMenuCategory=category},shape=RoundedCornerShape(14.dp),color=if(selected)Color(0xFF263C39) else Color(0xFF20242A),modifier=Modifier.weight(1f)) {
                        Column(Modifier.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(category.uppercase(),fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium,style=MaterialTheme.typography.labelSmall,color=if(selected)StudioAccent else Color(0xFFB6BDC7))
                            if(selected) Spacer(Modifier.height(3.dp)) else Spacer(Modifier.height(3.dp))
                            Box(Modifier.width(if(selected)24.dp else 8.dp).height(2.dp).background(if(selected)StudioAccent else Color.Transparent, RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                @Composable fun ToolTile(label:String, onClick:()->Unit, enabled:Boolean=true, accent:Boolean=false) {
                    Surface(onClick=onClick,enabled=enabled,shape=RoundedCornerShape(14.dp),color=if(accent)Color(0xFF263C39) else Color(0xFF20242A),border=androidx.compose.foundation.BorderStroke(1.dp,if(accent)StudioAccent.copy(alpha=.35f) else Color(0xFF343A43)) {
                        Column(Modifier.width(76.dp).padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text(label.take(2).uppercase(),fontWeight=FontWeight.Bold,color=if(accent)StudioAccent else Color(0xFFE4E8EC),style=MaterialTheme.typography.titleSmall)
                            Text(label,style=MaterialTheme.typography.labelSmall,color=if(enabled)Color(0xFFB9C0CA) else Color.Gray,maxLines=1)
                        }
                    }
                }
                when(activeMenuCategory) {
                    "Canvas" -> { ToolTile("Crop",onCropTransform,!beforeAfterVisible,true);ToolTile("Canvas",onCanvasSize,!beforeAfterVisible);ToolTile("Perspective",onPerspective,!beforeAfterVisible);ToolTile("Replace",onImportAnother,!beforeAfterVisible) }
                    "Color" -> { ToolTile("Adjust",onAdjustments,!beforeAfterVisible,true);ToolTile("Levels",onLevels,!beforeAfterVisible);ToolTile("Curves",onCurves,!beforeAfterVisible);ToolTile("HSL",onHsl,!beforeAfterVisible);ToolTile("Filters",onFilters,!beforeAfterVisible);ToolTile("Effects",onEffects,!beforeAfterVisible);ToolTile("LUT",onLut,!beforeAfterVisible) }
                    "Layers" -> { ToolTile("Layers",onLayers,!beforeAfterVisible,true);ToolTile("Brush",onBrush,!beforeAfterVisible);ToolTile("Select",onSelection,!beforeAfterVisible);ToolTile("Retouch",onRetouch,!beforeAfterVisible) }
                    "Design" -> { ToolTile("Text",onTextShape,!beforeAfterVisible,true);ToolTile("Gradient",onGradient,!beforeAfterVisible);ToolTile("BG Replace",onBackgroundReplace,!beforeAfterVisible) }
                    else -> { ToolTile("Color Key",onBackgroundRemove,!beforeAfterVisible,true);ToolTile("AI Remove",onAiBackgroundRemove,!beforeAfterVisible) }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF25282D)).padding(12.dp).pointerInput(bitmap) {
            detectTransformGestures { _, pan, gestureZoom, _ ->
                zoom = (zoom * gestureZoom).coerceIn(1f, 8f)
                if (zoom > 1f) { panX += pan.x; panY += pan.y } else { panX = 0f; panY = 0f }
            }
        }, contentAlignment = Alignment.Center) {
            Image((displayedBitmap?:bitmap).asImageBitmap(), contentDescription = if(beforeAfterVisible) "Before edit preview" else "Current edited canvas", modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = panX; translationY = panY }, contentScale = ContentScale.Fit)
        }
        Column(Modifier.fillMaxWidth().background(StudioSurface).padding(16.dp)) {
            Text("WinsPhoto Studio • Editor", fontWeight = FontWeight.Bold, color = StudioAccent)
            Text("Crop, transform, adjustments and undo are available. Source image is retained separately from the edit result.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun flattenForJpeg(source:Bitmap,backgroundColor:Int):Bitmap {
    require(source.width>0&&source.height>0&&source.width.toLong()*source.height<=24_000_000L){"JPEG flatten exceeds 24 MP safety limit"}
    val output=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
    try {
        val canvas=Canvas(output);canvas.drawColor(backgroundColor);canvas.drawBitmap(source,0f,0f,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG));return output
    } catch(error:Throwable) { if(!output.isRecycled)output.recycle();throw error }
}

private fun fitBitmapToCanvas(source:Bitmap,width:Int,height:Int):Bitmap {
    require(width>0&&height>0&&width.toLong()*height<=24_000_000L){"Layer fit exceeds 24 MP safety limit"}
    val output=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
    try {
        val canvas=Canvas(output)
        val scale=minOf(width.toFloat()/source.width,height.toFloat()/source.height)
        val drawW=(source.width*scale).toInt().coerceAtLeast(1);val drawH=(source.height*scale).toInt().coerceAtLeast(1)
        val left=(width-drawW)/2;val top=(height-drawH)/2
        canvas.drawBitmap(source,null,Rect(left,top,left+drawW,top+drawH),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return output
    } catch(error:Throwable) { if(!output.isRecycled)output.recycle();throw error }
}

private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sample = 1
    while (width / sample > maxDimension || height / sample > maxDimension) sample *= 2
    return sample
}