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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProjectScreen(source:Bitmap,onCancel:()->Unit,onOpen:(Bitmap,String)->Unit) {
    val context= androidx.compose.ui.platform.LocalContext.current
    val store=remember(context){ProjectStore(context)}
    val scope=rememberCoroutineScope()
    var name by remember { mutableStateOf("My Photo Project") }
    var projects by remember { mutableStateOf(store.listProjects()) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFF101216))) {
        Row(Modifier.fillMaxWidth().background(Color(0xFF1B1E24)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text("Projects",Modifier.weight(1f),color=Color.White)
            TextButton(onClick=onCancel){Text("Back")}
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedTextField(value=name,onValueChange={name=it},label={Text("Project name")},modifier=Modifier.fillMaxWidth())
            Button(enabled=!busy,onClick={busy=true;scope.launch {
                val saved=withContext(Dispatchers.IO){store.saveSnapshot(source,name)}
                projects=store.listProjects();busy=false;status="Saved: ${store.projectName(saved)}"
            }},modifier=Modifier.fillMaxWidth()) { if(busy) CircularProgressIndicator(Modifier.size(18.dp)) else Text("Save current project") }
            status?.let { Text(it,color=Color(0xFF77D7C0),modifier=Modifier.padding(top=8.dp)) }
        }
        Text("Saved projects",color=Color.White,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(horizontal=16.dp))
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(projects,key={it.name}) { folder ->
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF20242A))) {
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(store.projectName(folder),color=Color.White)
                            Text("Saved snapshot",color=Color.LightGray,style=MaterialTheme.typography.labelSmall)
                        }
                        TextButton(enabled=!busy,onClick={busy=true;scope.launch {
                            val loaded=withContext(Dispatchers.IO){store.loadSnapshot(folder)}
                            busy=false
                            if(loaded!=null) onOpen(loaded,store.projectName(folder)) else status="Could not open project snapshot"
                        }}){Text("Open")}
                        TextButton(onClick={folder.deleteRecursively();projects=store.listProjects()}){Text("Delete")}
                    }
                }
            }
        }
    }
}
