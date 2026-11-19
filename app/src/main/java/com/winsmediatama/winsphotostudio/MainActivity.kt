package com.winsmediatama.winsphotostudio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
    var layers by remember { mutableStateOf<List<EditorLayer>>(emptyList()) }
    var exportDialogOpen by remember { mutableStateOf(false) }
    var exportQuality by remember { mutableFloatStateOf(92f)
    }
    var exportFormat by remember { mutableStateOf("JPEG") }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var editHistory by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val jpegExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        val currentBitmap=bitmap
        if(uri!=null && currentBitmap!=null) scope.launch {
            val ok=withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.use { currentBitmap.compress(Bitmap.CompressFormat.JPEG,exportQuality.toInt(),it) } ?: false }.getOrDefault(false) }
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
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { imageUri = uri; loading = true; error = null }
    }
    LaunchedEffect(imageUri) {
        val selected = imageUri ?: return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(selected)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val sample = calculateSampleSize(bounds.outWidth, bounds.outHeight, 4096)
                val decodeOptions = Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
                context.contentResolver.openInputStream(selected)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            }.getOrNull()
        }
        loading = false
        bitmap?.let { loaded -> layers = listOf(EditorLayer(name="Background",bitmap=loaded)) }
        if (bitmap == null) error = "Unable to open image. Try JPG, PNG or WebP."
    }
    MaterialTheme(colorScheme = darkColorScheme(background = StudioBackground, surface = StudioSurface, primary = StudioAccent, onPrimary = Color(0xFF08231D), onBackground = Color.White, onSurface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = StudioBackground) {
            val current = bitmap
            if (current == null) HomeScreen(loading, error) { imagePicker.launch(arrayOf("image/*")) }
            else if (brushOpen) BrushScreen(
                source=current,
                onCancel={brushOpen=false},
                onApply={edited -> editHistory=editHistory+current;bitmap=edited;layers=listOf(EditorLayer(name="Background",bitmap=edited));brushOpen=false}
            )
            else if (projectsOpen) ProjectScreen(
                source=current,
                onCancel={projectsOpen=false},
                onOpen={loaded,projectName -> bitmap=loaded;layers=listOf(EditorLayer(name=projectName,bitmap=loaded));editHistory=emptyList();projectsOpen=false}
            )
            else if (layersOpen) LayersScreen(
                layers=layers,
                onCancel={layersOpen=false},
                onApply={newLayers -> layersOpen=false; scope.launch { val composite=withContext(Dispatchers.IO){LayerCompositeRenderer.render(newLayers,current.width,current.height)}; layers=newLayers; bitmap=composite }}
            )
            else if (curvesOpen) CurvesScreen(
                source=current,
                onCancel={curvesOpen=false},
                onApply={edited -> editHistory=editHistory+current; bitmap=edited; layers=listOf(EditorLayer(name="Background",bitmap=edited)); curvesOpen=false}
            )
            else if (levelsOpen) LevelsScreen(
                source=current,
                onCancel={levelsOpen=false},
                onApply={edited -> editHistory=editHistory+current; bitmap=edited; layers=listOf(EditorLayer(name="Background",bitmap=edited)); levelsOpen=false}
            )
            else if (adjustmentOpen) AdjustmentScreen(
                source = current,
                onCancel = { adjustmentOpen = false },
                onApply = { edited -> editHistory = editHistory + current; bitmap = edited; layers=listOf(EditorLayer(name="Background",bitmap=edited)); adjustmentOpen = false }
            )
            else if (cropTransformOpen) CropTransformScreen(
                source = current,
                onCancel = { cropTransformOpen = false },
                onApply = { edited ->
                    editHistory = editHistory + current
                    bitmap = edited
                    layers=listOf(EditorLayer(name="Background",bitmap=edited))
                    cropTransformOpen = false
                }
            )
            else EditorPreviewScreen(
                current,
                onBack = { bitmap = null; imageUri = null; editHistory = emptyList(); layers=emptyList() },
                onImportAnother = { imagePicker.launch(arrayOf("image/*")) },
                onCropTransform = { cropTransformOpen = true },
                onAdjustments = { adjustmentOpen = true },
                onLevels = { levelsOpen = true },
                onCurves = { curvesOpen = true },
                onLayers = { layersOpen = true },
                onProjects = { projectsOpen = true },
                onBrush = { brushOpen = true },
                onExport = { exportDialogOpen=true },
                onUndo = { if (editHistory.isNotEmpty()) { bitmap = editHistory.last(); layers=listOf(EditorLayer(name="Background",bitmap=editHistory.last())); editHistory = editHistory.dropLast(1) } },
                canUndo = editHistory.isNotEmpty()
            )
            if(exportDialogOpen) AlertDialog(
                onDismissRequest={exportDialogOpen=false},
                title={Text("Export image")},
                text={Column {
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("JPEG","PNG").forEach { fmt->FilterChip(selected=exportFormat==fmt,onClick={exportFormat=fmt},label={Text(fmt)}) } }
                    if(exportFormat=="JPEG") { Text("Quality: ${exportQuality.toInt()}%"); Slider(value=exportQuality,onValueChange={exportQuality=it},valueRange=50f..100f) }
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
private fun EditorPreviewScreen(bitmap: Bitmap, onBack: () -> Unit, onImportAnother: () -> Unit, onCropTransform: () -> Unit, onAdjustments: () -> Unit, onLevels: () -> Unit, onCurves: () -> Unit, onLayers: () -> Unit, onProjects: () -> Unit, onBrush: () -> Unit, onExport: () -> Unit, onUndo: () -> Unit, canUndo: Boolean) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(StudioSurface).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Column(Modifier.weight(1f)) {
                Text("Editor Preview", fontWeight = FontWeight.SemiBold)
                Text("${bitmap.width} × ${bitmap.height}px", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            }
            TextButton(onClick = { zoom = 1f; panX = 0f; panY = 0f }) { Text("Fit") }
            TextButton(onClick = onUndo, enabled = canUndo) { Text("Undo") }
            TextButton(onClick = onCropTransform) { Text("Crop / Transform") }
            TextButton(onClick = onAdjustments) { Text("Adjust") }
            TextButton(onClick = onLevels) { Text("Levels") }
            TextButton(onClick = onCurves) { Text("Curves") }
            TextButton(onClick = onLayers) { Text("Layers") }
            TextButton(onClick = onProjects) { Text("Project") }
            TextButton(onClick = onBrush) { Text("Brush") }
            TextButton(onClick = onExport) { Text("Export") }
            TextButton(onClick = onImportAnother) { Text("Replace") }
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

private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sample = 1
    while (width / sample > maxDimension || height / sample > maxDimension) sample *= 2
    return sample
}
