package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.ImageHistogram
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import com.winsmediatama.winsphotostudio.core.render.BitmapRenderScheduler
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@Composable
fun AdjustmentScreen(source: Bitmap, onCancel: () -> Unit, onApply: (Bitmap,BasicAdjustmentValues) -> Unit) {
    var values by remember { mutableStateOf(BasicAdjustmentValues()) }
    var resetKey by remember { mutableIntStateOf(0) }
    val renderScheduler = remember { BitmapRenderScheduler(maxCacheBytes = 0L, threadName = "WinsPhoto-AdjustPreview") }
    androidx.compose.runtime.DisposableEffect(renderScheduler) {
        onDispose { renderScheduler.close() }
    }
    val histogram by produceState<ImageHistogram?>(initialValue=null, source) {
        value = withContext(Dispatchers.Default) { ImageHistogram.from(source) }
    }
    val preview by produceState<Bitmap?>(initialValue=null, source, values, resetKey, renderScheduler) {
        val previousPreview=value
        value = null
        previousPreview?.takeIf { it !== source && !it.isRecycled }?.recycle()
        val cacheKey = "adjust:" + System.identityHashCode(source) + ":" + source.width + "x" + source.height + ":" + values.hashCode() + ":" + resetKey
        var request: Future<Bitmap>? = null
        try {
            value = withContext(Dispatchers.IO) {
                request = renderScheduler.request(cacheKey) { cancellation ->
                    cancellation.throwIfCanceled()
                    val result = BasicAdjustmentProcessor.apply(source, values, cancellation)
                    cancellation.throwIfCanceled()
                    result
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
    androidx.compose.runtime.DisposableEffect(preview){onDispose{preview?.takeIf{it!==source}?.recycle()}}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
            Text("Adjustments", Modifier.weight(1f), color=Color.White, fontWeight=FontWeight.Bold)
            TextButton(onClick={values=BasicAdjustmentValues(); resetKey++}) { Text("Reset") }
            TextButton(onClick=onCancel) { Text("Cancel") }
            Button(enabled=preview!=null, onClick={preview?.let{onApply(it.copy(Bitmap.Config.ARGB_8888,true),values)}}) { Text("Apply") }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)), contentAlignment=Alignment.Center) {
            preview?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize().padding(8.dp), contentScale=ContentScale.Fit) }
                ?: CircularProgressIndicator()
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max=340.dp).background(Color(0xFF1B1E24)), contentPadding=PaddingValues(16.dp)) {
            item {
                Text("RGB / Luminance Histogram", color=Color.White, fontWeight=FontWeight.SemiBold)
                HistogramPanel(histogram, Modifier.fillMaxWidth().height(112.dp).padding(vertical=8.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("R", color=Color(0xFFFF6B6B), style=MaterialTheme.typography.labelSmall)
                    Text("G", color=Color(0xFF68D391), style=MaterialTheme.typography.labelSmall)
                    Text("B", color=Color(0xFF6BA8FF), style=MaterialTheme.typography.labelSmall)
                    Text("Luma", color=Color.White, style=MaterialTheme.typography.labelSmall)
                    Text("Pixels: ${histogram?.sampleCount ?: 0}", color=Color.LightGray, style=MaterialTheme.typography.labelSmall)
                }
            }
            item { AdjustmentSlider("Brightness", values.brightness, -1f..1f) { values=values.copy(brightness=it) } }
            item { AdjustmentSlider("Contrast", values.contrast, -1f..1f) { values=values.copy(contrast=it) } }
            item { AdjustmentSlider("Exposure (EV)", values.exposure, -5f..5f) { values=values.copy(exposure=it) } }
            item { AdjustmentSlider("Saturation", values.saturation, -1f..1f) { values=values.copy(saturation=it) } }
            item { AdjustmentSlider("Hue", values.hue, -1f..1f) { values=values.copy(hue=it) } }
            item { AdjustmentSlider("Temperature", values.temperature, -1f..1f) { values=values.copy(temperature=it) } }
            item { AdjustmentSlider("Tint", values.tint, -1f..1f) { values=values.copy(tint=it) } }
            item { AdjustmentSlider("Red Balance", values.redBalance, -1f..1f) { values=values.copy(redBalance=it) } }
            item { AdjustmentSlider("Green Balance", values.greenBalance, -1f..1f) { values=values.copy(greenBalance=it) } }
            item { AdjustmentSlider("Blue Balance", values.blueBalance, -1f..1f) { values=values.copy(blueBalance=it) } }
            item { AdjustmentSlider("Highlights", values.highlights, -1f..1f) { values=values.copy(highlights=it) } }
            item { AdjustmentSlider("Shadows", values.shadows, -1f..1f) { values=values.copy(shadows=it) } }
            item { AdjustmentSlider("Whites", values.whites, -1f..1f) { values=values.copy(whites=it) } }
            item { AdjustmentSlider("Blacks", values.blacks, -1f..1f) { values=values.copy(blacks=it) } }
            item { AdjustmentSlider("Vibrance", values.vibrance, -1f..1f) { values=values.copy(vibrance=it) } }
            item { AdjustmentSlider("Clarity", values.clarity, -1f..1f) { values=values.copy(clarity=it) } }
            item { AdjustmentSlider("Sharpen", values.sharpen, 0f..1f) { values=values.copy(sharpen=it) } }
        }
    }
}

@Composable
private fun AdjustmentSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float)->Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
            Text(label, color=Color.White, style=MaterialTheme.typography.bodyMedium)
            Text("%.2f".format(value), color=Color.LightGray, style=MaterialTheme.typography.labelMedium)
        }
        Slider(value=value, onValueChange=onChange, valueRange=range)
    }
}


@Composable
private fun HistogramPanel(histogram: ImageHistogram?, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color(0xFF0B0D10))) {
        val data = histogram ?: return@Canvas
        val maxCount = maxOf(data.red.maxOrNull() ?: 0, data.green.maxOrNull() ?: 0, data.blue.maxOrNull() ?: 0, data.luminance.maxOrNull() ?: 0).coerceAtLeast(1)
        val binWidth = size.width / 256f
        fun drawBins(bins: IntArray, color: Color) {
            for (i in bins.indices) {
                val normalized = bins[i].toFloat() / maxCount
                if (normalized <= 0f) continue
                val barHeight = normalized * size.height
                drawRect(color.copy(alpha=0.42f), topLeft=androidx.compose.ui.geometry.Offset(i*binWidth, size.height-barHeight), size=androidx.compose.ui.geometry.Size(maxOf(1f,binWidth),barHeight))
            }
        }
        drawBins(data.red, Color(0xFFFF4545))
        drawBins(data.green, Color(0xFF39D86A))
        drawBins(data.blue, Color(0xFF438BFF))
        drawBins(data.luminance, Color.White.copy(alpha=0.5f))
    }
}
