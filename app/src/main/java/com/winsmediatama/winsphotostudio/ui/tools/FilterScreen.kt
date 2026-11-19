package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PhotoFilter(val name:String,val values:BasicAdjustmentValues)
private val filters=listOf(
    PhotoFilter("Original",BasicAdjustmentValues()),
    PhotoFilter("Vivid",BasicAdjustmentValues(contrast=.12f,saturation=.35f,vibrance=.25f,clarity=.12f)),
    PhotoFilter("Warm",BasicAdjustmentValues(temperature=.4f,saturation=.08f)),
    PhotoFilter("Cool",BasicAdjustmentValues(temperature=-.35f,tint=.08f,contrast=.08f)),
    PhotoFilter("Vintage",BasicAdjustmentValues(contrast=-.12f,saturation=-.25f,temperature=.22f,highlights=-.12f,shadows=.15f)),
    PhotoFilter("Cinematic",BasicAdjustmentValues(contrast=.22f,saturation=-.12f,temperature=-.08f,shadows=.18f,highlights=-.2f)),
    PhotoFilter("Soft",BasicAdjustmentValues(contrast=-.15f,brightness=.04f,saturation=-.12f,clarity=-.2f)),
    PhotoFilter("Mono",BasicAdjustmentValues(saturation=-1f,contrast=.12f)),
    PhotoFilter("Fade",BasicAdjustmentValues(contrast=-.22f,brightness=.08f,saturation=-.2f,whites=-.12f,blacks=.1f))
)

@Composable
fun FilterScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap,BasicAdjustmentValues)->Unit) {
    var selected by remember { mutableStateOf(filters.first()) }
    var strength by remember { mutableFloatStateOf(1f) }
    val preview by produceState<Bitmap?>(null,source,selected,strength) { value?.takeIf { it !== source }?.recycle(); value=null; value=withContext(Dispatchers.IO){BasicAdjustmentProcessor.apply(source,scaledFilterValues(selected.values,strength))} }
    androidx.compose.runtime.DisposableEffect(preview){onDispose{preview?.takeIf{it!==source}?.recycle()}}
    androidx.compose.runtime.DisposableEffect(preview){onDispose{preview?.recycle()}}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Filters",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let{onApply(it.copy(Bitmap.Config.ARGB_8888,true),scaledFilterValues(selected.values,strength))}}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center) {
            preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit)}?:CircularProgressIndicator()
        }
        LazyRow(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)),contentPadding=PaddingValues(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(filters) { filter ->
                FilterChip(selected=selected==filter,onClick={selected=filter;strength=1f},label={Text(filter.name)})
            }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(horizontal=12.dp, vertical=4.dp)) {
            Text("Strength ${(strength*100).toInt()}%",color=Color.LightGray)
            Slider(value=strength,onValueChange={strength=it},valueRange=0f..1f)
        }
        Text("Filter strength can be changed before applying.",color=Color.LightGray,modifier=Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall)
    }
}

private fun scaledFilterValues(v:BasicAdjustmentValues,s:Float)=BasicAdjustmentValues(
    brightness=v.brightness*s,contrast=v.contrast*s,exposure=v.exposure*s,saturation=v.saturation*s,hue=v.hue*s,
    temperature=v.temperature*s,tint=v.tint*s,redBalance=v.redBalance*s,greenBalance=v.greenBalance*s,blueBalance=v.blueBalance*s,
    highlights=v.highlights*s,shadows=v.shadows*s,whites=v.whites*s,blacks=v.blacks*s,vibrance=v.vibrance*s,
    clarity=v.clarity*s,sharpen=v.sharpen*s
)
