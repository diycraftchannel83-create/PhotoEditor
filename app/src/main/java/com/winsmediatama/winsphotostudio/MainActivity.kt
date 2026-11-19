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
import com.winsmediatama.winsphotostudio.ui.tools.TextShapeScreen
import com.winsmediatama.winsphotostudio.ui.tools.FilterScreen
import com.winsmediatama.winsphotostudio.ui.tools.RetouchScreen
import com.winsmediatama.winsphotostudio.ui.tools.BackgroundRemoveScreen
import com.winsmediatama.winsphotostudio.ui.tools.AiBackgroundRemoveScreen
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.editor.LayerCompositeRenderer
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

@Composable
private fun WinsPhotoApp() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cropTransformOpen by remember { mutableStateOf(false) }
    var adjustmentOpen by remember { mutableStateOf(false) }
    var levelsOpen by remember { mutableStateOf(false) }
    var curvesOpen by remember { mutableStateOf(false) }
    var layersOpen by remember { mutableStateOf(false) }
    var projectsOpen by remember { mutableStateOf(false) }
    var brushOpen by remember { mutableStateOf(false) }
    var selectionOpen by remember { mutableStateOf(false) }
    var textShapeOpen by remember { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }
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
    var editHistory by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    fun commitEdit(edited:Bitmap,previousComposite:Bitmap,flatten:Boolean=false) {
        editHistory=editHistory+previousComposite
        val selected=layers.firstOrNull{it.id==selectedLayerId}
        if(!flatten && selected!=null) {
            val updated=layers.map{if(it.id==selected.id)it.copy(bitmap=edited) else it}
            scope.launch {
                val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,previousComposite.width,previousComposite.height)}
                layers=updated;selectedLayerId=selected.id;bitmap=composite
            }
        } else {
            val newLayer=EditorLayer(name="Background",bitmap=edited)
            layers=listOf(newLayer);selectedLayerId=newLayer.id;bitmap=edited
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
                editHistory=editHistory+canvasBitmap
                val updated=layers+EditorLayer(name="Photo Layer ${layers.size+1}",bitmap=fitted)
                val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(updated,canvasBitmap.width,canvasBitmap.height)}
                layers=updated;bitmap=composite
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { imageUri = uri; loading = true; error = null; editHistory=emptyList() }
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
                onApply={edited -> commitEdit(edited,current);retouchOpen=false}
            )
            else if (filtersOpen) FilterScreen(
                source=selectedBitmap,
                onCancel={filtersOpen=false},
                onApply={edited -> commitEdit(edited,current);filtersOpen=false}
            )
            else if (textShapeOpen) TextShapeScreen(
                source=selectedBitmap,
                onCancel={textShapeOpen=false},
                onApply={edited -> commitEdit(edited,current);textShapeOpen=false}
            )
            else if (selectionOpen) SelectionScreen(
                source=selectedBitmap,
                onCancel={selectionOpen=false},
                onApply={edited -> commitEdit(edited,current,flatten=true);selectionOpen=false}
            )
            else if (brushOpen) BrushScreen(
                source=selectedBitmap,
                onCancel={brushOpen=false},
                onApply={edited -> commitEdit(edited,current);brushOpen=false}
            )
            else if (projectsOpen) ProjectScreen(
                source=current,
                onCancel={projectsOpen=false},
                onOpen={loaded,projectName -> bitmap=loaded;val layer=EditorLayer(name=projectName,bitmap=loaded);layers=listOf(layer);selectedLayerId=layer.id;editHistory=emptyList();projectsOpen=false}
            )
            else if (layersOpen) LayersScreen(
                layers=layers,
                selectedLayerId=selectedLayerId,
                onCancel={layersOpen=false},
                onAddImage={layerImagePicker.launch(arrayOf("image/*"))},
                onApply={newLayers,newSelected -> editHistory=editHistory+current;layersOpen=false;selectedLayerId=newSelected; scope.launch { val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(newLayers,current.width,current.height)}; layers=newLayers; bitmap=composite }}
            )
            else if (curvesOpen) CurvesScreen(
                source=selectedBitmap,
                onCancel={curvesOpen=false},
                onApply={edited -> commitEdit(edited,current);curvesOpen=false}
            )
            else if (levelsOpen) LevelsScreen(
                source=selectedBitmap,
                onCancel={levelsOpen=false},
                onApply={edited -> commitEdit(edited,current);levelsOpen=false}
            )
            else if (adjustmentOpen) AdjustmentScreen(
                source = selectedBitmap,
                onCancel = { adjustmentOpen = false },
                onApply = { edited -> commitEdit(edited,current);adjustmentOpen = false }
            )
            else if (cropTransformOpen) CropTransformScreen(
                source = current,
                onCancel = { cropTransformOpen = false },
                onApply = { edited ->
                    commitEdit(edited,current,flatten=true)
                    cropTransformOpen = false
                }
            )
            else EditorPreviewScreen(
                current,
                onBack = { bitmap = null; imageUri = null; editHistory = emptyList(); layers=emptyList();selectedLayerId=null },
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
                onRetouch = { retouchOpen = true },
                onBackgroundRemove = { backgroundRemoveOpen = true },
                onAiBackgroundRemove = { aiBackgroundRemoveOpen = true },
                onExport = { exportDialogOpen=true },
                onUndo = { if (editHistory.isNotEmpty()) { bitmap = editHistory.last();val restored=EditorLayer(name="Background",bitmap=editHistory.last());layers=listOf(restored);selectedLayerId=restored.id;editHistory = editHistory.dropLast(1) } },
                canUndo = editHistory.isNotEmpty()
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
            exportStatus?.let { status-> AlertDialog(onDismissRequest={exportStatus=null},title={Text("Export")},text={Text(status)},confirmButton={TextButton(onClick={exportStatus=null}){Text("OK")}}) }
        }
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
private fun EditorPreviewScreen(bitmap: Bitmap, onBack: () -> Unit, onImportAnother: () -> Unit, onCropTransform: () -> Unit, onAdjustments: () -> Unit, onLevels: () -> Unit, onCurves: () -> Unit, onLayers: () -> Unit, onProjects: () -> Unit, onBrush: () -> Unit, onSelection: () -> Unit, onTextShape: () -> Unit, onFilters: () -> Unit, onRetouch: () -> Unit, onBackgroundRemove: () -> Unit, onAiBackgroundRemove: () -> Unit, onExport: () -> Unit, onUndo: () -> Unit, canUndo: Boolean) {
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
                TextButton(onClick=onUndo,enabled=canUndo){Text("Undo")}
                TextButton(onClick=onExport){Text("Export")}
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
