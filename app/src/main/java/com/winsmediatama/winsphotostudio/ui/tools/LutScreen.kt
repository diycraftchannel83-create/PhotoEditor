package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.winsmediatama.winsphotostudio.core.adjustments.CubeLut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import com.winsmediatama.winsphotostudio.core.render.BitmapRenderScheduler
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@Composable
fun LutScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap,String)->Unit) {
    val context=LocalContext.current
    var lutText by remember{mutableStateOf<String?>(null)}
    var title by remember{mutableStateOf("No LUT loaded")}
    var error by remember{mutableStateOf<String?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null){
            error=null
            runCatching{context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()} ?: error("Unable to read LUT")}
                .onSuccess{content->runCatching{CubeLut.parse(content)}.onSuccess{lutText=content;title=uri.lastPathSegment?: "Imported LUT"}.onFailure{error=it.message?:"LUT parse failed"}}
                .onFailure{error=it.message?: "Invalid LUT file"}
        }
    }
    val renderScheduler=remember{BitmapRenderScheduler(48L*1024L*1024L,"WinsPhoto-LutPreview")}
    DisposableEffect(renderScheduler){onDispose{renderScheduler.close()}}
    val preview by produceState<Bitmap?>(null,source,lutText,renderScheduler){
        value?.takeIf{it!==source&&!it.isRecycled}?.recycle();value=null
        val text=lutText
        if(text!=null){
            val key="lut:"+System.identityHashCode(source)+":"+source.width+"x"+source.height+":"+text.hashCode()
            var request:Future<Bitmap>?=null
            try{value=withContext(Dispatchers.IO){request=renderScheduler.request(key){signal->signal.throwIfCanceled();CubeLut.parse(text).apply(source).also{signal.throwIfCanceled()}};request!!.get(60,TimeUnit.SECONDS)}}
            catch(cancelled:CancellationException){request?.cancel(true);throw cancelled}
            catch(_:Exception){request?.cancel(true);value=null}
        }
    }
    DisposableEffect(preview){onDispose{preview?.takeIf{it!==source&&!it.isRecycled}?.recycle()}}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))){
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
            Text("LUT Import",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let{onApply(it.copy(Bitmap.Config.ARGB_8888,true),lutText.orEmpty())}}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center){
            preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit)}?:Text("Import a .cube LUT to preview",color=Color.LightGray)
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(16.dp)){
            Text(title,color=Color.LightGray)
            Button(onClick={picker.launch(arrayOf("text/plain","application/octet-stream","*/*"))},modifier=Modifier.fillMaxWidth()){Text("Import .cube LUT")}
            Text("Supports common 1D and 3D .cube files. Processing is offline.",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }
    }
}
