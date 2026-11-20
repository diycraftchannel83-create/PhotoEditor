package com.winsmediatama.winsphotostudio.ui.tools

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.winsmediatama.winsphotostudio.core.storage.ProjectStore
import com.winsmediatama.winsphotostudio.core.storage.LoadedStudioProject
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProjectScreen(source:Bitmap,layers:List<EditorLayer>,selectedLayerId:String?,onCancel:()->Unit,onSaved:()->Unit={},onOpen:(LoadedStudioProject)->Unit) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val store=remember(context){ProjectStore(context)}
    val scope=rememberCoroutineScope()
    var name by remember{mutableStateOf("My Photo Project")}
    var projects by remember{mutableStateOf(store.listProjects())}
    var status by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    var deleteTarget by remember{mutableStateOf<File?>(null)}
    var renameTarget by remember{mutableStateOf<File?>(null)}
    var renameName by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))){
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){Text("Projects",Modifier.weight(1f),color=Color.White);TextButton(onClick=onCancel){Text("Back")}}
        Column(Modifier.fillMaxWidth().padding(16.dp)){
            OutlinedTextField(value=name,onValueChange={name=it},label={Text("Project name")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Button(enabled=!busy&&layers.isNotEmpty(),onClick={busy=true;scope.launch{
                val result=runCatching{withContext(Dispatchers.IO){store.saveLayerProject(layers,selectedLayerId,source.width,source.height,name)}}
                busy=false
                result.onSuccess{folder->projects=store.listProjects();status="Saved: ${store.projectName(folder)}";onSaved()}.onFailure{status="Save failed: ${it.message?:"Unknown error"}"}
            }},modifier=Modifier.fillMaxWidth()){if(busy)CircularProgressIndicator(Modifier.size(18.dp))else Text("Save current project")}
            status?.let{Text(it,color=Color(0xFF77D7C0),modifier=Modifier.padding(top=8.dp))}
        }
        Text("Saved projects",color=Color.White,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(horizontal=16.dp))
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            items(projects,key={it.name}){folder->
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF20242A))){
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){Text(store.projectName(folder),color=Color.White);Text("Editable layer stack",color=Color.LightGray,style=MaterialTheme.typography.labelSmall)}
                        TextButton(enabled=!busy,onClick={busy=true;scope.launch{
                            val result=runCatching{withContext(Dispatchers.IO){store.loadEditableProject(folder)}}
                            busy=false
                            result.onSuccess{loaded->if(loaded!=null)onOpen(loaded)else status="Could not open project"}.onFailure{status="Open failed: ${it.message?:"Unknown error"}"}
                        }}){Text("Open")}
                        TextButton(enabled=!busy,onClick={renameTarget=folder;renameName=store.projectName(folder)}){Text("Rename")}
                        TextButton(enabled=!busy,onClick={busy=true;scope.launch{
                            val result=withContext(Dispatchers.IO){store.duplicateProject(folder)}
                            busy=false
                            if(result!=null){projects=store.listProjects();status="Project duplicated"}else status="Duplicate failed"
                        }}){Text("Copy")}
                        TextButton(enabled=!busy,onClick={deleteTarget=folder}){Text("Delete")}
                    }
                }
            }
        }
    }
    renameTarget?.let{target->AlertDialog(onDismissRequest={renameTarget=null},title={Text("Rename project")},text={OutlinedTextField(value=renameName,onValueChange={renameName=it},singleLine=true,label={Text("New name")})},confirmButton={TextButton(onClick={val ok=store.renameProject(target,renameName);projects=store.listProjects();status=if(ok)"Project renamed" else "Rename failed";renameTarget=null}){Text("Save")}},dismissButton={TextButton(onClick={renameTarget=null}){Text("Cancel")}})}
    deleteTarget?.let{target->AlertDialog(onDismissRequest={deleteTarget=null},title={Text("Delete project?")},text={Text("Permanently delete ${store.projectName(target)} and its assets?")},confirmButton={TextButton(onClick={target.deleteRecursively();projects=store.listProjects();status="Project deleted";deleteTarget=null}){Text("Delete")}},dismissButton={TextButton(onClick={deleteTarget=null}){Text("Cancel")}})}
}
