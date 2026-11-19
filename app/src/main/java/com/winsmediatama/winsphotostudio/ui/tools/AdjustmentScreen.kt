package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AdjustmentScreen(source: Bitmap, onCancel: () -> Unit, onApply: (Bitmap) -> Unit) {
    var values by remember { mutableStateOf(BasicAdjustmentValues()) }
    var resetKey by remember { mutableIntStateOf(0) }
    val preview by produceState<Bitmap?>(initialValue=null, source, values, resetKey) {
        value = null
        value = withContext(Dispatchers.IO) { BasicAdjustmentProcessor.apply(source, values) }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
            Text("Adjustments", Modifier.weight(1f), color=Color.White, fontWeight=FontWeight.Bold)
            TextButton(onClick={values=BasicAdjustmentValues(); resetKey++}) { Text("Reset") }
            TextButton(onClick=onCancel) { Text("Cancel") }
            Button(enabled=preview!=null, onClick={preview?.let(onApply)}) { Text("Apply") }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)), contentAlignment=Alignment.Center) {
            preview?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize().padding(8.dp), contentScale=ContentScale.Fit) }
                ?: CircularProgressIndicator()
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max=340.dp).background(Color(0xFF1B1E24)), contentPadding=PaddingValues(16.dp)) {
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
