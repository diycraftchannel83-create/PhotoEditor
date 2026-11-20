package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.render.BitmapRenderScheduler
import com.winsmediatama.winsphotostudio.core.tools.CanvasAnchor
import com.winsmediatama.winsphotostudio.core.tools.CanvasResizeProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

private const val CANVAS_SIZE_PIXEL_LIMIT = 24_000_000L
private const val CANVAS_SIZE_DIMENSION_LIMIT = 8192

@Composable
fun CanvasSizeScreen(source: Bitmap, onCancel: () -> Unit, onApply: (Bitmap, Int, Int, CanvasAnchor, Int) -> Unit) {
    var width by remember { mutableFloatStateOf(source.width.toFloat()) }
    var height by remember { mutableFloatStateOf(source.height.toFloat()) }
    var anchor by remember { mutableStateOf(CanvasAnchor.CENTER) }
    var background by remember { mutableStateOf(Color.Transparent) }
    val anchors = CanvasAnchor.entries
    val outputWidth = width.toInt().coerceAtLeast(1)
    val outputHeight = height.toInt().coerceAtLeast(1)
    val validOutput = outputWidth <= CANVAS_SIZE_DIMENSION_LIMIT && outputHeight <= CANVAS_SIZE_DIMENSION_LIMIT &&
        outputWidth.toLong() * outputHeight.toLong() <= CANVAS_SIZE_PIXEL_LIMIT
    val scheduler = remember { BitmapRenderScheduler(32L * 1024L * 1024L, "WinsPhoto-CanvasPreview") }
    DisposableEffect(scheduler) { onDispose { scheduler.close() } }

    val preview by produceState<Bitmap?>(null, source, outputWidth, outputHeight, anchor, background, validOutput, scheduler) {
        value?.takeIf { it !== source && !it.isRecycled }?.recycle()
        value = null
        if (!validOutput) return@produceState
        val key = "canvas-size:${System.identityHashCode(source)}:${source.width}x${source.height}:${outputWidth}x${outputHeight}:${anchor.name}:${background.toArgb()}"
        var request: Future<Bitmap>? = null
        try {
            value = withContext(Dispatchers.IO) {
                request = scheduler.request(key) { signal ->
                    signal.throwIfCanceled()
                    CanvasResizeProcessor.resize(source, outputWidth, outputHeight, anchor, background.toArgb()).also { signal.throwIfCanceled() }
                }
                request!!.get(60, TimeUnit.SECONDS)
            }
        } catch (cancelled: CancellationException) {
            request?.cancel(true)
            throw cancelled
        } catch (_: Exception) {
            request?.cancel(true)
            value = null
        }
    }
    DisposableEffect(preview) { onDispose { preview?.takeIf { it !== source && !it.isRecycled }?.recycle() } }

    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Canvas Size", Modifier.weight(1f), color = Color.White)
            TextButton(onClick = onCancel) { Text("Cancel") }
            Button(enabled = preview != null && validOutput, onClick = {
                preview?.takeIf { !it.isRecycled }?.let {
                    onApply(it.copy(Bitmap.Config.ARGB_8888, true), outputWidth, outputHeight, anchor, background.toArgb())
                }
            }) { Text("Apply") }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)), contentAlignment = Alignment.Center) {
            if (!validOutput) {
                Text("Output is too large. Limit: 24 MP and 8192 px per side.", color = Color(0xFFFF8A80))
            } else {
                preview?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit) }
                    ?: CircularProgressIndicator()
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Width: $outputWidth px", color = Color.LightGray)
            Slider(value = width, onValueChange = { width = it }, valueRange = 1f..minOf(CANVAS_SIZE_DIMENSION_LIMIT.toFloat(), source.width * 3f).coerceAtLeast(1f))
            Text("Height: $outputHeight px", color = Color.LightGray)
            Slider(value = height, onValueChange = { height = it }, valueRange = 1f..minOf(CANVAS_SIZE_DIMENSION_LIMIT.toFloat(), source.height * 3f).coerceAtLeast(1f))
            Text("Output: ${outputWidth} × ${outputHeight} px (${(outputWidth.toLong() * outputHeight / 1_000_000f).let { String.format("%.2f", it) } } MP)", color = Color.LightGray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { width = source.width.toFloat(); height = source.height.toFloat() }) { Text("Reset") }
                OutlinedButton(onClick = { val temp = width; width = height; height = temp }) { Text("Swap") }
            }
            Text("Anchor", color = Color.White)
            anchors.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { item ->
                        FilterChip(selected = anchor == item, onClick = { anchor = item }, label = { Text(item.name.replace("_", " "), maxLines = 1) })
                    }
                }
            }
            Text("Background", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Color.Transparent, Color.White, Color.Black).forEach { color ->
                    Button(onClick = { background = color }, colors = ButtonDefaults.buttonColors(containerColor = if (color == Color.Transparent) Color(0xFF555555) else color), modifier = Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) {
                        if (background == color) Text("✓", color = if (color == Color.Black) Color.White else Color.Black)
                    }
                }
            }
        }
    }
}
