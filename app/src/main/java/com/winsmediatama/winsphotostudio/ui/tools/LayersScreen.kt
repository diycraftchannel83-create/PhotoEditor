package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
fun LayersScreen(
    layers:List<EditorLayer>, selectedLayerId:String?, onCancel:()->Unit,
    onAddImage:()->Unit, onEditMask:(String,Bitmap)->Unit,
    onApply:(List<EditorLayer>,String?)->Unit
) {
    var working by remember(layers) { mutableStateOf(layers) }
    var selectedId by remember(layers,selectedLayerId) { mutableStateOf(selectedLayerId ?: layers.lastOrNull()?.id) }
    var groupSelection by remember(layers) { mutableStateOf(setOf<String>()) }
    var status by remember { mutableStateOf<String?>(null) }
    var groupNameInput by remember { mutableStateOf("") }
    var groupDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Layers",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onAddImage){Text("+ Photo")}
            TextButton(onClick=onCancel){Text("Cancel")}
            Button(onClick={onApply(working,selectedId)}){Text("Apply")}
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal=6.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick={val base=working.firstOrNull()?.bitmap;val blank=Bitmap.createBitmap(base?.width?:1,base?.height?:1,Bitmap.Config.ARGB_8888);val layer=EditorLayer(name="Layer ${working.size+1}",bitmap=blank);working=working+layer;selectedId=layer.id}){Text("+ Empty")}
            OutlinedButton(enabled=groupSelection.size>=2,onClick={val indexes=working.indices.filter{working[it].id in groupSelection};if(indexes.size<2)status="Choose at least two layers" else if(indexes.last()-indexes.first()+1!=indexes.size)status="Group requires adjacent layers" else {groupDialog=true}}){Text("Group")}
            OutlinedButton(onClick={val id=working.firstOrNull{it.id in groupSelection}?.groupId?:working.firstOrNull{it.id==selectedId}?.groupId;if(id==null)status="Select a grouped layer" else {working=working.map{if(it.groupId==id)it.copy(groupId=null,groupName=null,groupOpacity=1f)else it};groupSelection=emptySet();status="Ungrouped"}}){Text("Ungroup")}
            OutlinedButton(onClick={groupSelection=working.filter{it.groupId!=null}.map{it.id}.toSet();status="All grouped layers selected"}){Text("Select groups")}
        }
        status?.let{Text(it,color=Color(0xFF77D7C0),modifier=Modifier.padding(horizontal=12.dp,vertical=4.dp))}
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            itemsIndexed(working.asReversed(),key={_,l->l.id}) { reverseIndex, layer ->
                val index=working.lastIndex-reverseIndex
                var rename by remember(layer.id){mutableStateOf(layer.name)}
                var blendMenu by remember(layer.id){mutableStateOf(false)}
                var groupBlendMenu by remember(layer.id){mutableStateOf(false)}
                Card(colors=CardDefaults.cardColors(containerColor=if(selectedId==layer.id)Color(0xFF263B38) else Color(0xFF20242A))) {
                    Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Checkbox(checked=layer.id in groupSelection,onCheckedChange={v->groupSelection=if(v)groupSelection+layer.id else groupSelection-layer.id})
                            Image(layer.bitmap.asImageBitmap(),null,Modifier.size(54.dp),contentScale=ContentScale.Crop)
                            Column(Modifier.weight(1f).padding(start=8.dp)) {
                                OutlinedTextField(value=rename,onValueChange={rename=it},singleLine=true,label={Text("Name")},modifier=Modifier.fillMaxWidth())
                                Text(buildString{append(if(layer.visible)"Visible" else "Hidden");append(" • ");append(if(layer.locked)"Locked" else "Unlocked");append(" • ");append(if(layer.alphaLocked)"Alpha locked" else "Alpha free");append(" • ");append(if(layer.clipToBelow)"Clipped" else "Unclipped")},color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
                            }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick={working=working.toMutableList().also{it[index]=layer.copy(name=rename)}}){Text("Rename")}
                            OutlinedButton(onClick={selectedId=layer.id}){Text(if(selectedId==layer.id)"Selected" else "Select")}
                            OutlinedButton(onClick={working=working.toMutableList().also{it[index]=layer.copy(locked=!layer.locked)}}){Text(if(layer.locked)"Unlock" else "Lock")}
                            OutlinedButton(onClick={working=working.toMutableList().also{it[index]=layer.copy(alphaLocked=!layer.alphaLocked)}}){Text(if(layer.alphaLocked)"Alpha off" else "Alpha lock")}
                            OutlinedButton(enabled=index>0,onClick={working=working.toMutableList().also{it[index]=layer.copy(clipToBelow=!layer.clipToBelow)}}){Text(if(layer.clipToBelow)"Unclip" else "Clip")}
                            Checkbox(checked=layer.visible,onCheckedChange={v->working=working.toMutableList().also{it[index]=layer.copy(visible=v)}})
                        }
                        if(layer.groupId!=null) {
                            Text("Group: ${layer.groupName?: "Group"}",color=Color(0xFF77D7C0),style=MaterialTheme.typography.labelSmall)
                            Row(verticalAlignment=Alignment.CenterVertically){
                                TextButton(onClick={groupBlendMenu=true}){Text("Group blend: ${(layer.groupBlendMode?:EditorBlendMode.NORMAL).name}")}
                                DropdownMenu(expanded=groupBlendMenu,onDismissRequest={groupBlendMenu=false}){
                                    EditorBlendMode.entries.forEach{mode->DropdownMenuItem(text={Text(mode.name)},onClick={working=working.map{if(it.groupId==layer.groupId)it.copy(groupBlendMode=mode)else it};groupBlendMenu=false})}
                                }
                            }
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Text("Group opacity ${(layer.groupOpacity*100).toInt()}%",color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
                                Slider(value=layer.groupOpacity,onValueChange={v->working=working.map{if(it.groupId==layer.groupId)it.copy(groupOpacity=v)else it}},modifier=Modifier.weight(1f))
                                TextButton(onClick={val group=working.filter{it.groupId==layer.groupId};val makeVisible=group.any{!it.visible};working=working.map{if(it.groupId==layer.groupId)it.copy(visible=makeVisible)else it}}){Text(if(working.filter{it.groupId==layer.groupId}.all{it.visible})"Hide" else "Show")}
                            }
                        }
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            TextButton(onClick={blendMenu=true}){Text("Blend: ${layer.blendMode.name}")}
                            DropdownMenu(expanded=blendMenu,onDismissRequest={blendMenu=false}) {
                                EditorBlendMode.entries.forEach{mode->DropdownMenuItem(text={Text(mode.name)},onClick={working=working.toMutableList().also{it[index]=layer.copy(blendMode=mode)};blendMenu=false})}
                            }
                        }
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("Opacity ${(layer.opacity*100).toInt()}%",color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
                            Slider(value=layer.opacity,onValueChange={v->working=working.toMutableList().also{it[index]=layer.copy(opacity=v)}},modifier=Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick={val m=LayerMaskProcessor.createWhiteMask(layer.bitmap.width,layer.bitmap.height);working=working.toMutableList().also{it[index]=layer.copy(mask=m)}}){Text(if(layer.mask==null)"Add mask" else "Reset")}
                            OutlinedButton(enabled=layer.mask!=null,onClick={layer.mask?.let{onEditMask(layer.id,it)}}){Text("Paint")}
                            OutlinedButton(enabled=layer.mask!=null,onClick={val m=layer.mask?.let{LayerMaskProcessor.invert(it)};working=working.toMutableList().also{it[index]=layer.copy(mask=m)}}){Text("Invert")}
                            OutlinedButton(enabled=layer.mask!=null,onClick={working=working.toMutableList().also{it[index]=layer.copy(mask=null)}}){Text("Remove")}
                        }
                        layer.mask?.let{mask->Row(verticalAlignment=Alignment.CenterVertically){Image(mask.asImageBitmap(),null,Modifier.size(36.dp),contentScale=ContentScale.Crop);Text("Mask active",color=Color.LightGray,modifier=Modifier.padding(start=8.dp))}}
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(enabled=index<working.lastIndex,onClick={working=working.toMutableList().also{val item=it.removeAt(index);it.add(index+1,item)}},modifier=Modifier.weight(1f)){Text("Up")}
                            OutlinedButton(enabled=index>0,onClick={working=working.toMutableList().also{val item=it.removeAt(index);it.add(index-1,item)}},modifier=Modifier.weight(1f)){Text("Down")}
                            OutlinedButton(onClick={val duplicate=layer.copy(id=java.util.UUID.randomUUID().toString(),name=layer.name+" Copy",bitmap=layer.bitmap.copy(Bitmap.Config.ARGB_8888,true),mask=layer.mask?.copy(Bitmap.Config.ARGB_8888,true));working=working.toMutableList().also{it.add(index+1,duplicate)};selectedId=duplicate.id},modifier=Modifier.weight(1f)){Text("Duplicate")}
                            OutlinedButton(enabled=working.size>1,onClick={working=working.toMutableList().also{it.removeAt(index)};if(selectedId==layer.id)selectedId=working.lastOrNull()?.id;groupSelection=groupSelection-layer.id},modifier=Modifier.weight(1f)){Text("Delete")}
                        }
                    }
                }
            }
        }
    }
    if(groupDialog)AlertDialog(onDismissRequest={groupDialog=false},title={Text("Create layer group")},text={Column{Text("Name your group");OutlinedTextField(value=groupNameInput,onValueChange={groupNameInput=it},singleLine=true)}} ,confirmButton={TextButton(onClick={val indexes=working.indices.filter{working[it].id in groupSelection};if(indexes.size>=2&&indexes.last()-indexes.first()+1==indexes.size){val id=java.util.UUID.randomUUID().toString();val name=groupNameInput.ifBlank{"Group ${working.count{it.groupId!=null}+1}"};working=working.map{if(it.id in groupSelection)it.copy(groupId=id,groupName=name,groupOpacity=1f)else it};groupSelection=emptySet();status="Created $name"}else status="Selection must be contiguous";groupDialog=false;groupNameInput=""}){Text("Create")}},dismissButton={TextButton(onClick={groupDialog=false}){Text("Cancel")}})
}
