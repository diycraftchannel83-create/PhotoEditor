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
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mlkit.vision.common.InputImage
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

private data class SegmentationResult(val width:Int,val height:Int,val mask:FloatArray)
private data class AiMaskState(val result:SegmentationResult?,val error:String?)

@Composable
fun AiBackgroundRemoveScreen(source:Bitmap,onCancel:()->Unit,onApply:(Bitmap)->Unit) {
    var threshold by remember { mutableFloatStateOf(.35f) }
    var softness by remember { mutableFloatStateOf(.25f) }
    val segmentation by produceState<AiMaskState>(AiMaskState(null,null),source) {
        value=withContext(Dispatchers.IO) {
            runCatching {
                val options=SelfieSegmenterOptions.Builder().setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE).enableRawSizeMask().build()
                val segmenter=Segmentation.getClient(options)
                try {
                    val result=Tasks.await(segmenter.process(InputImage.fromBitmap(source,0)))
                    val mask=result.getMask()
                    val buffer=mask.getBuffer().duplicate()
                    buffer.rewind()
                    val data=FloatArray(mask.getWidth()*mask.getHeight())
                    for(i in data.indices)data[i]=buffer.get().coerceIn(0f,1f)
                    SegmentationResult(mask.getWidth(),mask.getHeight(),data)
                } finally { segmenter.close() }
            }.fold({AiMaskState(it,null)},{AiMaskState(null,it.message ?: "Segmentation failed")})
        }
    }
    val preview by produceState<Bitmap?>(null,source,segmentation.result,threshold,softness) {
        val previousPreview=value
        value=null
        previousPreview?.takeIf { it !== source }?.recycle()
        value?.takeIf { it !== source }?.recycle()
        value=null
        value=withContext(Dispatchers.IO){segmentation.result?.let{applyAiMask(source,it,threshold,softness)}}
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("AI Background Removal",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(enabled=preview!=null,onClick={preview?.let(onApply)}){Text("Apply")}
        }
        Box(Modifier.weight(1f).fillMaxWidth().aspectRatio(source.width.toFloat()/source.height).padding(12.dp).background(Color(0xFF25282D)),contentAlignment=Alignment.Center) {
            preview?.let{Image(it.asImageBitmap(),null,Modifier.fillMaxSize().padding(8.dp),contentScale=ContentScale.Fit)}
                ?: if(segmentation.error!=null) Text(segmentation.error,color=Color(0xFFFF8080)) else CircularProgressIndicator()
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp)) {
            Text("Offline person segmentation. Best for photos containing people.",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
            Text("Mask threshold ${(threshold*100).toInt()}%",color=Color.White);Slider(value=threshold,onValueChange={threshold=it},valueRange=0f..0.8f)
            Text("Edge softness ${(softness*100).toInt()}%",color=Color.White);Slider(value=softness,onValueChange={softness=it},valueRange=0f..0.6f)
        }
    }
}

private fun applyAiMask(source:Bitmap,result:SegmentationResult,threshold:Float,softness:Float):Bitmap {
    val output=source.copy(Bitmap.Config.ARGB_8888,true)
    val pixels=IntArray(source.width*source.height);output.getPixels(pixels,0,source.width,0,0,source.width,source.height)
    for(y in 0 until source.height)for(x in 0 until source.width) {
        val mx=(x.toFloat()/source.width*result.width).toInt().coerceIn(0,result.width-1)
        val my=(y.toFloat()/source.height*result.height).toInt().coerceIn(0,result.height-1)
        val confidence=result.mask[my*result.width+mx]
        val alpha=if(softness<=0f)if(confidence>=threshold)1f else 0f else smoothstep(threshold-softness,threshold+softness,confidence)
        val i=y*source.width+x;pixels[i]=(pixels[i] and 0x00FFFFFF) or (alpha*255f).toInt().coerceIn(0,255).shl(24)
    }
    output.setPixels(pixels,0,source.width,0,0,source.width,source.height)
    return output
}
private fun smoothstep(edge0:Float,edge1:Float,x:Float):Float {
    if(edge1<=edge0)return if(x>=edge1)1f else 0f
    val t=((x-edge0)/(edge1-edge0)).coerceIn(0f,1f)
    return t*t*(3f-2f*t)
}
