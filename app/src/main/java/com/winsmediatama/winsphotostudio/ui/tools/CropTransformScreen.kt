package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private enum class CropPreset(val title: String, val ratio: Float?) {
    ORIGINAL("Original", null), FREE("Free", null), SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f/3f), THREE_TWO("3:2", 3f/2f),
    SIXTEEN_NINE("16:9", 16f/9f), NINE_SIXTEEN("9:16", 9f/16f)
}

private data class TransformSettings(
    val cropLeft: Float = 0f, val cropTop: Float = 0f,
    val cropRight: Float = 1f, val cropBottom: Float = 1f,
    val rotation: Int = 0, val flipH: Boolean = false, val flipV: Boolean = false,
    val outputWidth: Int = 0, val outputHeight: Int = 0
)

@Composable
fun CropTransformScreen(source: Bitmap, onCancel: () -> Unit, onApply: (Bitmap) -> Unit) {
    var preset by remember { mutableStateOf(CropPreset.ORIGINAL) }
    var settings by remember { mutableStateOf(TransformSettings(outputWidth = source.width, outputHeight = source.height)) }
    var widthText by remember { mutableStateOf(source.width.toString()) }
    var heightText by remember { mutableStateOf(source.height.toString()) }
    var showResize by remember { mutableStateOf(false) }
    val rendered = remember(source, settings) { renderCropTransform(source, settings) }
    val sourceRatio = source.width.toFloat() / source.height.coerceAtLeast(1)

    fun setPreset(p: CropPreset) {
        preset = p
        val ratio = p.ratio ?: if (p == CropPreset.ORIGINAL) sourceRatio else null
        if (ratio == null) {
            settings = settings.copy(cropLeft=0f, cropTop=0f, cropRight=1f, cropBottom=1f)
        } else {
            val (cw,ch) = if (ratio > sourceRatio) 1f to (sourceRatio/ratio) else (ratio/sourceRatio) to 1f
            settings = settings.copy(cropLeft=(1f-cw)/2f, cropTop=(1f-ch)/2f, cropRight=(1f+cw)/2f, cropBottom=(1f+ch)/2f)
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
            Text("Crop & Transform", Modifier.weight(1f), fontWeight=FontWeight.Bold, color=Color.White)
            TextButton(onClick=onCancel) { Text("Cancel") }
            Button(onClick={ onApply(rendered) }) { Text("Apply") }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D), RoundedCornerShape(12.dp)), contentAlignment=Alignment.Center) {
            Image(rendered.asImageBitmap(), contentDescription="Transform preview", modifier=Modifier.fillMaxSize().padding(8.dp), contentScale=ContentScale.Fit)
        }
        Text("Crop ratio", color=Color.White, fontWeight=FontWeight.SemiBold, modifier=Modifier.padding(start=16.dp, top=4.dp))
        LazyRow(contentPadding=PaddingValues(horizontal=12.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(CropPreset.entries) { p -> FilterChip(selected=preset==p, onClick={setPreset(p)}, label={Text(p.title)}) }
        }
        Text("Free crop area", color=Color.LightGray, modifier=Modifier.padding(horizontal=16.dp, vertical=4.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Width ${((settings.cropRight-settings.cropLeft)*100).roundToInt()}%", color=Color.LightGray, style=MaterialTheme.typography.labelSmall)
                Slider(value=settings.cropRight-settings.cropLeft, onValueChange={ v ->
                    val size=v.coerceIn(0.15f,1f); val center=(settings.cropLeft+settings.cropRight)/2f
                    val left=(center-size/2).coerceIn(0f,1f-size)
                    settings=settings.copy(cropLeft=left,cropRight=left+size); preset=CropPreset.FREE
                }, valueRange=0.15f..1f)
            }
            Column(Modifier.weight(1f)) {
                Text("Height ${((settings.cropBottom-settings.cropTop)*100).roundToInt()}%", color=Color.LightGray, style=MaterialTheme.typography.labelSmall)
                Slider(value=settings.cropBottom-settings.cropTop, onValueChange={ v ->
                    val size=v.coerceIn(0.15f,1f); val center=(settings.cropTop+settings.cropBottom)/2f
                    val top=(center-size/2).coerceIn(0f,1f-size)
                    settings=settings.copy(cropTop=top,cropBottom=top+size); preset=CropPreset.FREE
                }, valueRange=0.15f..1f)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick={ settings=settings.copy(rotation=(settings.rotation-90+360)%360) }, modifier=Modifier.weight(1f)) { Text("↶ 90°") }
            OutlinedButton(onClick={ settings=settings.copy(rotation=(settings.rotation+90)%360) }, modifier=Modifier.weight(1f)) { Text("↷ 90°") }
            OutlinedButton(onClick={settings=settings.copy(flipH=!settings.flipH)}, modifier=Modifier.weight(1f)) { Text("Flip H") }
            OutlinedButton(onClick={settings=settings.copy(flipV=!settings.flipV)}, modifier=Modifier.weight(1f)) { Text("Flip V") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
            Text("Output: ${rendered.width} × ${rendered.height}px", color=Color.LightGray, style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={showResize=true}) { Text("Resize…") }
            TextButton(onClick={setPreset(CropPreset.ORIGINAL); settings=TransformSettings(outputWidth=source.width,outputHeight=source.height); widthText=source.width.toString(); heightText=source.height.toString()}) { Text("Reset") }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showResize) AlertDialog(onDismissRequest={showResize=false}, title={Text("Resize image")}, text={
        Column {
            OutlinedTextField(value=widthText,onValueChange={widthText=it.filter(Char::isDigit)},label={Text("Width (px)")},singleLine=true)
            OutlinedTextField(value=heightText,onValueChange={heightText=it.filter(Char::isDigit)},label={Text("Height (px)")},singleLine=true)
            Text("Resize applies to the cropped/rotated result.",style=MaterialTheme.typography.bodySmall)
        }
    }, confirmButton={TextButton(onClick={
        val w=widthText.toIntOrNull()?.coerceIn(1,12000) ?: source.width
        val h=heightText.toIntOrNull()?.coerceIn(1,12000) ?: source.height
        settings=settings.copy(outputWidth=w,outputHeight=h); showResize=false
    }){Text("Apply size")}}, dismissButton={TextButton(onClick={showResize=false}){Text("Cancel")}})
}

private fun renderCropTransform(bitmap: Bitmap, settings: TransformSettings): Bitmap {
    val left=(settings.cropLeft*bitmap.width).roundToInt().coerceIn(0,bitmap.width-1)
    val top=(settings.cropTop*bitmap.height).roundToInt().coerceIn(0,bitmap.height-1)
    val right=(settings.cropRight*bitmap.width).roundToInt().coerceIn(left+1,bitmap.width)
    val bottom=(settings.cropBottom*bitmap.height).roundToInt().coerceIn(top+1,bitmap.height)
    var cropped=Bitmap.createBitmap(bitmap,left,top,right-left,bottom-top)
    if(settings.rotation!=0 || settings.flipH || settings.flipV) {
        val matrix=Matrix().apply { postRotate(settings.rotation.toFloat()); postScale(if(settings.flipH)-1f else 1f,if(settings.flipV)-1f else 1f) }
        val transformed=Bitmap.createBitmap(cropped,0,0,cropped.width,cropped.height,matrix,true)
        if(transformed!==cropped) cropped.recycle()
        cropped=transformed
    }
    val w=settings.outputWidth.coerceIn(1,12000); val h=settings.outputHeight.coerceIn(1,12000)
    if(cropped.width==w && cropped.height==h) return cropped
    val resized=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
    Canvas(resized).drawBitmap(cropped,null,android.graphics.Rect(0,0,w,h),Paint(Paint.FILTER_BITMAP_FLAG))
    cropped.recycle()
    return resized
}
