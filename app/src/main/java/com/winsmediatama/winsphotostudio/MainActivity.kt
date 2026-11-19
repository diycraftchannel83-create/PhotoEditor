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
    var editHistory by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { imageUri = uri; loading = true; error = null }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
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
        if (bitmap == null) error = "Unable to open image. Try JPG, PNG or WebP."
    }
    MaterialTheme(colorScheme = darkColorScheme(background = StudioBackground, surface = StudioSurface, primary = StudioAccent, onPrimary = Color(0xFF08231D), onBackground = Color.White, onSurface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = StudioBackground) {
            val current = bitmap
            if (current == null) HomeScreen(loading, error) { imagePicker.launch(arrayOf("image/*")) }
            else if (cropTransformOpen) CropTransformScreen(
                source = current,
                onCancel = { cropTransformOpen = false },
                onApply = { edited ->
                    editHistory = editHistory + current
                    bitmap = edited
                    cropTransformOpen = false
                }
            )
            else EditorPreviewScreen(
                current,
                onBack = { bitmap = null; imageUri = null; editHistory = emptyList() },
                onImportAnother = { imagePicker.launch(arrayOf("image/*")) },
                onCropTransform = { cropTransformOpen = true },
                onUndo = { if (editHistory.isNotEmpty()) { bitmap = editHistory.last(); editHistory = editHistory.dropLast(1) } },
                canUndo = editHistory.isNotEmpty()
            )
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
private fun EditorPreviewScreen(bitmap: Bitmap, onBack: () -> Unit, onImportAnother: () -> Unit, onCropTransform: () -> Unit, onUndo: () -> Unit, canUndo: Boolean) {
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
            Text("Crop, rotate, flip, resize and undo are available. Source image is retained separately from the edit result.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sample = 1
    while (width / sample > maxDimension || height / sample > maxDimension) sample *= 2
    return sample
}
