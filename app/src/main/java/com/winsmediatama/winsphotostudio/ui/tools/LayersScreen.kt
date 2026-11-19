package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.editor.EditorBlendMode
import com.winsmediatama.winsphotostudio.core.editor.LayerMaskProcessor

@Composable
fun LayersScreen(layers:List<EditorLayer>,selectedLayerId:String?,onCancel:()->Unit,onAddImage:()->Unit,onEditMask:(String,Bitmap)->Unit,onApply:(List<EditorLayer>,String?)->Unit) {
    var working by remember(layers) { mutableStateOf(layers) }
    var selectedId by remember(layers,selectedLayerId) { mutableStateOf(selectedLayerId ?: layers.lastOrNull()?.id) }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Layers",Modifier.weight(1f),color=Color.White)
            TextButton(onClick={val newLayer=EditorLayer(name="Layer ${working.size+1}",bitmap=Bitmap.createBitmap(working.firstOrNull()?.bitmap?.width ?: 1,working.firstOrNull()?.bitmap?.height ?: 1,Bitmap.Config.ARGB_8888));working=working+newLayer;selectedId=newLayer.id}){Text("+ Empty")}
            TextButton(onClick=onAddImage){Text("+ Photo")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(onClick={onApply(working,selectedId)}){Text("Apply")}
        }
        Text("Bottom-to-top stacking • Tap visibility • Adjust opacity",color=Color.LightGray,modifier=Modifier.padding(12.dp),style=MaterialTheme.typography.bodySmall)
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            itemsIndexed(working.asReversed(),key={_,l->l.id}) { reverseIndex, layer ->
                val index=working.lastIndex-reverseIndex
                var rename by remember(layer.id) { mutableStateOf(layer.name) }
                var blendMenu by remember(layer.id) { mutableStateOf(false) }
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF20242A))) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Image(layer.bitmap.asImageBitmap(),null,Modifier.size(56.dp),contentScale=ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start=8.dp)) {
                                OutlinedTextField(value=rename,onValueChange={rename=it},singleLine=true,label={Text("Layer name")})
                                Text((if(layer.visible)"Visible" else "Hidden")+" • "+(if(layer.locked)"Locked" else "Unlocked")+" • "+(if(layer.alphaLocked)"Alpha Locked" else "Alpha Free"),color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick={working=working.toMutableList().also{it[index]=layer.copy(name=rename)}}){Text("Rename")}
                            TextButton(onClick={selectedId=layer.id}){Text(if(selectedId==layer.id)"Selected" else "Select")}
                            TextButton(onClick={working=working.toMutableList().also{it[index]=layer.copy(locked=!layer.locked)}}){Text(if(layer.locked)"Unlock" else "Lock")}
                            TextButton(onClick={working=working.toMutableList().also{it[index]=layer.copy(alphaLocked=!layer.alphaLocked)}}){Text(if(layer.alphaLocked)"Alpha Unlocked" else "Alpha Lock")}
                            Checkbox(checked=layer.visible,onCheckedChange={v->working=working.toMutableList().also{it[index]=layer.copy(visible=v)}})
                        }
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            TextButton(onClick={blendMenu=true}){Text("Blend: ${layer.blendMode.name}")}
                            DropdownMenu(expanded=blendMenu,onDismissRequest={blendMenu=false}) {
                                EditorBlendMode.entries.forEach { mode->DropdownMenuItem(text={Text(mode.name)},onClick={working=working.toMutableList().also{it[index]=layer.copy(blendMode=mode)};blendMenu=false}) }
                            }
                        }
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("Opacity ${(layer.opacity*100).toInt()}%",color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
                            Slider(value=layer.opacity,onValueChange={v->working=working.toMutableList().also{it[index]=layer.copy(opacity=v)}},modifier=Modifier.weight(1f))
                        }
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick={val mask=LayerMaskProcessor.createWhiteMask(layer.bitmap.width,layer.bitmap.height);working=working.toMutableList().also{it[index]=layer.copy(mask=mask)}}){Text(if(layer.mask==null)"Add Mask" else "Reset Mask")}
                            OutlinedButton(enabled=layer.mask!=null,onClick={layer.mask?.let{onEditMask(layer.id,it)}}){Text("Paint Mask")}
                            OutlinedButton(enabled=layer.mask!=null,onClick={val m=layer.mask?.let{LayerMaskProcessor.invert(it)};working=working.toMutableList().also{it[index]=layer.copy(mask=m)}}){Text("Invert")}
                            OutlinedButton(enabled=layer.mask!=null,onClick={working=working.toMutableList().also{it[index]=layer.copy(mask=null)}}){Text("Remove Mask")}
                        }
                        layer.mask?.let{mask->Row(verticalAlignment=Alignment.CenterVertically){Image(mask.asImageBitmap(),null,Modifier.size(36.dp),contentScale=ContentScale.Crop);Text("Layer mask active",color=Color.LightGray,modifier=Modifier.padding(start=8.dp))}}
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(enabled=index<working.lastIndex,onClick={working=working.toMutableList().also{val item=it.removeAt(index);it.add(index+1,item)}},modifier=Modifier.weight(1f)){Text("Up")}
                            OutlinedButton(enabled=index>0,onClick={working=working.toMutableList().also{val item=it.removeAt(index);it.add(index-1,item)}},modifier=Modifier.weight(1f)){Text("Down")}
                            OutlinedButton(onClick={val duplicate=layer.copy(id=java.util.UUID.randomUUID().toString(),name=layer.name+" Copy",bitmap=layer.bitmap.copy(Bitmap.Config.ARGB_8888,true));working=working.toMutableList().also{it.add(index+1,duplicate)};selectedId=duplicate.id},modifier=Modifier.weight(1f)){Text("Duplicate")}
                            OutlinedButton(enabled=working.size>1,onClick={working=working.toMutableList().also{it.removeAt(index)};if(selectedId==layer.id)selectedId=working.lastOrNull()?.id},modifier=Modifier.weight(1f)){Text("Delete")}
                        }
                    }
                }
            }
        }
    }
}
