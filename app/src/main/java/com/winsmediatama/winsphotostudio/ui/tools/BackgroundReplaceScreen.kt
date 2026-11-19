package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BackgroundReplaceScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    val context=LocalContext.current
    var startColor by remember{mutableStateOf(Color(0xFF77D7C0))}
    var endColor by remember{mutableStateOf(Color(0xFF102A43))}
    var gradient by remember{mutableStateOf(true)}
    var imported by remember{mutableStateOf<Bitmap?>(null)}
    var status by remember{mutableStateOf<String?>(null)}
    val hasTransparency by produceState<Boolean?>(null,source){value=withContext(Dispatchers.IO){detectTransparency(source)}}
    val palette=listOf(Color.White,Color.Black,Color(0xFFFF3B30),Color(0xFFFFC107),Color(0xFF00C853),Color(0xFF00B0FF),Color(0xFF7C4DFF),Color(0xFFFF4081),Color(0xFF77D7C0))
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)runCatching{context.contentResolver.openInputStream(uri)?.use{stream->val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true};android.graphics.BitmapFactory.decodeStream(stream,null,bounds);val sample=calculateBackgroundSample(bounds.outWidth,bounds.outHeight,2048);context.contentResolver.openInputStream(uri)?.use{input->android.graphics.BitmapFactory.decodeStream(input,null,android.graphics.BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888})}}}
            .onSuccess{decoded->if(decoded!=null){imported=decoded;status="Photo background loaded"}else status="Unable to decode selected image"}
            .onFailure{status=it.message}
    }
    val preview by produceState<Bitmap?>(null,source,startColor,endColor,gradient,imported){
        value?.takeIf{it!==source}?.recycle();value=null
        value=withContext(Dispatchers.Default){composeBackground(source,imported,startColor.toArgb(),endColor.toArgb(),gradient)}
    }
    androidx.compose.runtime.DisposableEffect(preview,imported){onDispose{preview?.takeIf{it!==source}?.recycle();imported?.takeIf{it!==source}?.recycle()}}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))){
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
            Text("Replace Background",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let{onApply(it.copy(Bitmap.Config.ARGB_8888,true))}}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center){
            preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit)}?:CircularProgressIndicator()
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)){
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(selected=gradient,onClick={gradient=true;imported=null},label={Text("Gradient")});FilterChip(selected=!gradient&&imported==null,onClick={gradient=false;imported=null},label={Text("Solid color")});Button(onClick={picker.launch(arrayOf("image/*"))}){Text("Photo")}}
            Text("Start / solid color",color=Color.White);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){palette.forEach{c->Button(onClick={startColor=c},colors=ButtonDefaults.buttonColors(containerColor=c),modifier=Modifier.size(30.dp),contentPadding=PaddingValues(0.dp)){} }}
            if(gradient){Text("End color",color=Color.White);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){palette.forEach{c->Button(onClick={endColor=c},colors=ButtonDefaults.buttonColors(containerColor=c),modifier=Modifier.size(30.dp),contentPadding=PaddingValues(0.dp)){} }}}
            if(hasTransparency==false)Text("Source background is opaque. Remove the background first for a clean replacement.",color=Color(0xFFFFC078),style=MaterialTheme.typography.bodySmall)
            status?.let{Text(it,color=Color.LightGray)}
        }
    }
}
private fun composeBackground(source:Bitmap,background:Bitmap?,start:Int,end:Int,gradient:Boolean):Bitmap {
    val w=source.width;val h=source.height
    val out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);val canvas=AndroidCanvas(out)
    if(background!=null){val scale=maxOf(w.toFloat()/background.width,h.toFloat()/background.height);val bw=background.width*scale;val bh=background.height*scale;canvas.drawBitmap(background,null,android.graphics.RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG))}
    else {val paint=AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG);paint.shader=if(gradient)LinearGradient(0f,0f,w.toFloat(),h.toFloat(),start,end,Shader.TileMode.CLAMP)else null;canvas.drawColor(if(gradient)android.graphics.Color.TRANSPARENT else start);if(gradient)canvas.drawRect(0f,0f,w.toFloat(),h.toFloat(),paint);paint.shader=null}
    canvas.drawBitmap(source,0f,0f,AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG))
    return out
}

private fun detectTransparency(bitmap:Bitmap):Boolean {
    val stride=(maxOf(bitmap.width,bitmap.height)/128).coerceAtLeast(1)
    for(y in 0 until bitmap.height step stride)for(x in 0 until bitmap.width step stride)if(android.graphics.Color.alpha(bitmap.getPixel(x,y))<250)return true
    return false
}

private fun calculateBackgroundSample(width:Int,height:Int,maxDimension:Int):Int {
    if(width<=0||height<=0)return 1
    var sample=1
    while(width/sample>maxDimension||height/sample>maxDimension)sample*=2
    return sample
}
