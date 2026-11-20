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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.adjustments.EffectProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.PhotoEffectKind
import com.winsmediatama.winsphotostudio.core.adjustments.PhotoEffectSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import com.winsmediatama.winsphotostudio.core.render.BitmapRenderScheduler
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit



@Composable
fun EffectsScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap,PhotoEffectSettings)->Unit) {
    var kind by remember{mutableStateOf(PhotoEffectKind.VIGNETTE)}
    var strength by remember{mutableFloatStateOf(.5f)}
    var angle by remember{mutableFloatStateOf(45f)}
    val renderScheduler=remember{BitmapRenderScheduler(48L*1024L*1024L,"WinsPhoto-EffectsPreview")}
    DisposableEffect(renderScheduler){onDispose{renderScheduler.close()}}
    val preview by produceState<Bitmap?>(null,source,kind,strength,angle,renderScheduler) {
        value?.takeIf{it!==source&&!it.isRecycled}?.recycle();value=null
        val key="effect:"+System.identityHashCode(source)+":"+source.width+"x"+source.height+":"+kind.name+":"+strength+":"+angle
        var request:Future<Bitmap>?=null
        try {
            value=withContext(Dispatchers.IO) {
                request=renderScheduler.request(key){ cancellation ->
                    cancellation.throwIfCanceled()
                    val result=when(kind){
                        PhotoEffectKind.BLUR->EffectProcessor.gaussianBlur(source,(strength*24).toInt(),cancellation)
                        PhotoEffectKind.MOTION->if(strength<=0f)source.copy(Bitmap.Config.ARGB_8888,true) else EffectProcessor.motionBlur(source,(strength*24).toInt().coerceAtLeast(1),angle,cancellation)
                        PhotoEffectKind.VIGNETTE->EffectProcessor.vignette(source,strength,cancellation)
                        PhotoEffectKind.GRAIN->EffectProcessor.grain(source,strength,seed=101,cancellation=cancellation)
                    }
                    cancellation.throwIfCanceled()
                    result
                }
                request!!.get(60,TimeUnit.SECONDS)
            }
        } catch(cancelled:CancellationException){request?.cancel(true);throw cancelled}
        catch(_:Exception){request?.cancel(true);value=null}
    }
    androidx.compose.runtime.DisposableEffect(preview){onDispose{preview?.takeUnless{it===source||it.isRecycled}?.recycle()}}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
            Text("Effects",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let{onApply(it.copy(Bitmap.Config.ARGB_8888,true),PhotoEffectSettings(kind,strength,angle))}}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center){
            preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit)}?:CircularProgressIndicator()
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)){
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){PhotoEffectKind.entries.forEach{e->FilterChip(selected=kind==e,onClick={kind=e;strength=.5f},label={Text(e.name)})}}
            Text("Strength ${(strength*100).toInt()}%",color=Color.LightGray)
            Slider(value=strength,onValueChange={strength=it},valueRange=0f..1f)
            if(kind==PhotoEffectKind.MOTION){Text("Angle ${angle.toInt()}°",color=Color.LightGray);Slider(value=angle,onValueChange={angle=it},valueRange=0f..360f)}
        }
    }
}
