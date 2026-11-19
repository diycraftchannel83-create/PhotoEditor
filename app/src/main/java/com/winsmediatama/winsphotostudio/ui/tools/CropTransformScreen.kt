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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class CropPreset(val title:String,val ratio:Float?) {
    ORIGINAL("Original",null),FREE("Free",null),SQUARE("1:1",1f),FOUR_THREE("4:3",4f/3f),
    THREE_TWO("3:2",3f/2f),SIXTEEN_NINE("16:9",16f/9f),NINE_SIXTEEN("9:16",9f/16f)
}
private data class TransformSettings(
    val cropLeft:Float=0f,val cropTop:Float=0f,val cropRight:Float=1f,val cropBottom:Float=1f,
    val rotation:Int=0,val flipH:Boolean=false,val flipV:Boolean=false,
    val outputWidth:Int=0,val outputHeight:Int=0,val customResize:Boolean=false
)

@Composable
fun CropTransformScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var preset by remember { mutableStateOf(CropPreset.ORIGINAL) }
    var settings by remember { mutableStateOf(TransformSettings(outputWidth=source.width,outputHeight=source.height)) }
    var centerX by remember { mutableFloatStateOf(.5f) }
    var centerY by remember { mutableFloatStateOf(.5f) }
    var widthText by remember { mutableStateOf(source.width.toString()) }
    var heightText by remember { mutableStateOf(source.height.toString()) }
    var showResize by remember { mutableStateOf(false) }
    val preview by produceState<Bitmap?>(null,source,settings) { value?.takeIf { it !== source }?.recycle(); value=null; value=withContext(Dispatchers.IO){renderCropTransform(source,settings)} }
    val sourceRatio=source.width.toFloat()/source.height.coerceAtLeast(1)

    fun updateCrop(l:Float,t:Float,r:Float,b:Float,markFree:Boolean=true) {
        val cl=l.coerceIn(0f,1f);val ct=t.coerceIn(0f,1f);val cr=r.coerceIn(cl+.01f,1f);val cb=b.coerceIn(ct+.01f,1f)
        val cw=((cr-cl)*source.width).roundToInt().coerceAtLeast(1);val ch=((cb-ct)*source.height).roundToInt().coerceAtLeast(1)
        val rotated=settings.rotation%180!=0
        settings=settings.copy(cropLeft=cl,cropTop=ct,cropRight=cr,cropBottom=cb,
            outputWidth=if(settings.customResize)settings.outputWidth else if(rotated)ch else cw,
            outputHeight=if(settings.customResize)settings.outputHeight else if(rotated)cw else ch,
            customResize=settings.customResize)
        centerX=(cl+cr)/2f;centerY=(ct+cb)/2f;if(markFree)preset=CropPreset.FREE
    }
    fun setPreset(p:CropPreset) {
        preset=p
        val ratio=p.ratio ?: if(p==CropPreset.ORIGINAL) sourceRatio else null
        if(ratio==null) { settings=settings.copy(customResize=false);centerX=.5f;centerY=.5f;updateCrop(0f,0f,1f,1f,false);settings=settings.copy(customResize=false) }
        else {
            val (cw,ch)=if(ratio>sourceRatio) 1f to (sourceRatio/ratio) else (ratio/sourceRatio) to 1f
            centerX=.5f;centerY=.5f
            settings=settings.copy(customResize=false)
            updateCrop((1f-cw)/2f,(1f-ch)/2f,(1f+cw)/2f,(1f+ch)/2f,false)
            settings=settings.copy(customResize=false)
        }
    }
    fun rotate(delta:Int) {
        val next=(settings.rotation+delta+360)%360
        val rotated=next%180!=0
        val wasRotated=settings.rotation%180!=0
        val cropW=((settings.cropRight-settings.cropLeft)*source.width).roundToInt().coerceAtLeast(1)
        val cropH=((settings.cropBottom-settings.cropTop)*source.height).roundToInt().coerceAtLeast(1)
        settings=settings.copy(rotation=next,outputWidth=if(settings.customResize) if(rotated!=wasRotated)settings.outputHeight else settings.outputWidth else if(rotated)cropH else cropW,
            outputHeight=if(settings.customResize) if(rotated!=wasRotated)settings.outputWidth else settings.outputHeight else if(rotated)cropW else cropH)
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Crop & Transform",Modifier.weight(1f),fontWeight=FontWeight.Bold,color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let(onApply)}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D),RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
            preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit)}?:CircularProgressIndicator()
        }
        Text("Crop ratio",color=Color.White,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(start=16.dp,top=4.dp))
        LazyRow(contentPadding=PaddingValues(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(CropPreset.entries){p->FilterChip(selected=preset==p,onClick={setPreset(p)},label={Text(p.title)})}}
        Text("Crop size",color=Color.LightGray,modifier=Modifier.padding(horizontal=16.dp,vertical=4.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Width ${((settings.cropRight-settings.cropLeft)*100).roundToInt()}%",color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
                Slider(value=settings.cropRight-settings.cropLeft,onValueChange={v->val sz=v.coerceIn(.1f,1f);updateCrop((centerX-sz/2).coerceIn(0f,1f-sz),settings.cropTop,(centerX+sz/2).coerceIn(sz,1f),settings.cropBottom)},valueRange=.1f..1f)
            }
            Column(Modifier.weight(1f)) {
                Text("Height ${((settings.cropBottom-settings.cropTop)*100).roundToInt()}%",color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
                Slider(value=settings.cropBottom-settings.cropTop,onValueChange={v->val sz=v.coerceIn(.1f,1f);updateCrop(settings.cropLeft,(centerY-sz/2).coerceIn(0f,1f-sz),settings.cropRight,(centerY+sz/2).coerceIn(sz,1f))},valueRange=.1f..1f)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)){Text("Horizontal position",color=Color.LightGray,style=MaterialTheme.typography.labelSmall);Slider(value=centerX,onValueChange={v->val w=settings.cropRight-settings.cropLeft;updateCrop((v-w/2).coerceIn(0f,1f-w),settings.cropTop,(v+w/2).coerceIn(w,1f),settings.cropBottom)},valueRange=0f..1f)}
            Column(Modifier.weight(1f)){Text("Vertical position",color=Color.LightGray,style=MaterialTheme.typography.labelSmall);Slider(value=centerY,onValueChange={v->val h=settings.cropBottom-settings.cropTop;updateCrop(settings.cropLeft,(v-h/2).coerceIn(0f,1f-h),settings.cropRight,(v+h/2).coerceIn(h,1f),settings.cropBottom)},valueRange=0f..1f)}
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick={rotate(-90)},modifier=Modifier.weight(1f)){Text("↶ 90°")}
            OutlinedButton(onClick={rotate(90)},modifier=Modifier.weight(1f)){Text("↷ 90°")}
            OutlinedButton(onClick={settings=settings.copy(flipH=!settings.flipH)},modifier=Modifier.weight(1f)){Text("Flip H")}
            OutlinedButton(onClick={settings=settings.copy(flipV=!settings.flipV)},modifier=Modifier.weight(1f)){Text("Flip V")}
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("Output: ${preview?.width ?: settings.outputWidth} × ${preview?.height ?: settings.outputHeight}px",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
            TextButton(onClick={widthText=(preview?.width?:settings.outputWidth).toString();heightText=(preview?.height?:settings.outputHeight).toString();showResize=true}){Text("Resize")}
            TextButton(onClick={setPreset(CropPreset.ORIGINAL);settings=TransformSettings(outputWidth=source.width,outputHeight=source.height);centerX=.5f;centerY=.5f;widthText=source.width.toString();heightText=source.height.toString()}){Text("Reset")}
        }
        Spacer(Modifier.height(4.dp))
    }
    if(showResize) AlertDialog(onDismissRequest={showResize=false},title={Text("Resize image")},text={Column {
        OutlinedTextField(value=widthText,onValueChange={widthText=it.filter(Char::isDigit)},label={Text("Width (px)")},singleLine=true)
        OutlinedTextField(value=heightText,onValueChange={heightText=it.filter(Char::isDigit)},label={Text("Height (px)")},singleLine=true)
        Text("Resize applies after crop and rotation. Max dimension: 2048 px.",style=MaterialTheme.typography.bodySmall)
    }},confirmButton={TextButton(onClick={val w=widthText.toIntOrNull()?.coerceIn(1,2048)?:source.width;val h=heightText.toIntOrNull()?.coerceIn(1,2048)?:source.height;settings=settings.copy(outputWidth=w,outputHeight=h,customResize=true);showResize=false}){Text("Apply size")}},dismissButton={TextButton(onClick={showResize=false}){Text("Cancel")}})
}

private fun renderCropTransform(bitmap:Bitmap,s:TransformSettings):Bitmap {
    val l=(s.cropLeft*bitmap.width).roundToInt().coerceIn(0,bitmap.width-1)
    val t=(s.cropTop*bitmap.height).roundToInt().coerceIn(0,bitmap.height-1)
    val r=(s.cropRight*bitmap.width).roundToInt().coerceIn(l+1,bitmap.width)
    val b=(s.cropBottom*bitmap.height).roundToInt().coerceIn(t+1,bitmap.height)
    var crop=Bitmap.createBitmap(bitmap,l,t,r-l,b-t)
    if(crop===bitmap) crop=bitmap.copy(Bitmap.Config.ARGB_8888,true)
    if(s.rotation!=0||s.flipH||s.flipV) {
        val matrix=Matrix().apply{postRotate(s.rotation.toFloat());postScale(if(s.flipH)-1f else 1f,if(s.flipV)-1f else 1f)}
        val transformed=Bitmap.createBitmap(crop,0,0,crop.width,crop.height,matrix,true)
        if(transformed!==crop)crop.recycle();crop=transformed
    }
    val w=s.outputWidth.coerceIn(1,2048);val h=s.outputHeight.coerceIn(1,2048)
    if(crop.width==w&&crop.height==h)return crop
    val resized=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
    Canvas(resized).drawBitmap(crop,null,android.graphics.Rect(0,0,w,h),Paint(Paint.FILTER_BITMAP_FLAG));crop.recycle();return resized
}
