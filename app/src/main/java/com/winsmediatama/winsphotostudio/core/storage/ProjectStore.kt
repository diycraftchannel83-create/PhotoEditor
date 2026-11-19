package com.winsmediatama.winsphotostudio.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack
import com.winsmediatama.winsphotostudio.core.editor.EditorBlendMode

/** Local project persistence. Assets are copied into the app-owned project folder; originals are untouched. */
data class LoadedStudioProject(val name:String,val canvasWidth:Int,val canvasHeight:Int,val layers:List<EditorLayer>,val selectedLayerId:String?)

class ProjectStore(context: Context) {
    private val root = File(context.filesDir, "studio-projects").apply { mkdirs() }

    fun createProjectFromUri(context: Context, uri: Uri, projectName: String, width: Int, height: Int): File {
        val id = UUID.randomUUID().toString()
        val folder = File(root, id).apply { mkdirs() }
        val assets = File(folder, "assets").apply { mkdirs() }
        val imageFile = File(assets, "source-image")
        context.contentResolver.openInputStream(uri)?.use { input -> imageFile.outputStream().use { output -> input.copyTo(output) } }
            ?: throw IllegalStateException("Cannot open selected image URI")
        val manifest = JSONObject()
            .put("schemaVersion", 1)
            .put("projectId", id)
            .put("name", projectName)
            .put("canvasWidth", width)
            .put("canvasHeight", height)
            .put("createdAt", System.currentTimeMillis())
            .put("updatedAt", System.currentTimeMillis())
            .put("sourceAsset", "assets/source-image")
            .put("layers", JSONArray().put(JSONObject().put("id", "background").put("name", "Background").put("visible", true).put("locked", true).put("opacity", 1.0).put("blendMode", "NORMAL")))
        File(folder, "project.json").writeText(manifest.toString(2))
        return folder
    }


    /** Save the current rendered canvas as a reopenable project snapshot without altering the source import. */
    fun saveSnapshot(bitmap: Bitmap, projectName: String): File {
        val id = UUID.randomUUID().toString()
        val folder = File(root, id).apply { mkdirs() }
        val assets = File(folder, "assets").apply { mkdirs() }
        val snapshot = File(assets, "canvas.png")
        snapshot.outputStream().use { if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,it))throw IllegalStateException("Failed to write snapshot asset") }
        val manifest = JSONObject()
            .put("schemaVersion", 1)
            .put("projectId", id)
            .put("name", projectName.trim().ifBlank { "Untitled Project" })
            .put("canvasWidth", bitmap.width)
            .put("canvasHeight", bitmap.height)
            .put("createdAt", System.currentTimeMillis())
            .put("updatedAt", System.currentTimeMillis())
            .put("snapshotAsset", "assets/canvas.png")
            .put("layers", JSONArray().put(JSONObject().put("id", "snapshot").put("name", "Rendered Snapshot").put("visible", true).put("locked", true).put("opacity", 1.0).put("blendMode", "NORMAL")))
        File(folder, "project.json").writeText(manifest.toString(2))
        return folder
    }

    fun loadSnapshot(projectFolder: File): Bitmap? = runCatching {
        val manifest = readManifest(projectFolder)
        val path = manifest.optString("snapshotAsset", "assets/canvas.png")
        decodeAsset(projectFolder, path)
    }.getOrNull()

    fun projectName(projectFolder: File): String = readManifest(projectFolder).optString("name", projectFolder.name)

    fun saveLayerProject(layers:List<EditorLayer>,selectedLayerId:String?,canvasWidth:Int,canvasHeight:Int,projectName:String):File {
        val id=UUID.randomUUID().toString()
        val folder=File(root,id).apply{mkdirs()}
        File(folder,"assets").mkdirs()
        val layerArray=JSONArray()
        layers.forEachIndexed{index,layer->
            val assetPath="assets/layer_$index.png"
            File(folder,assetPath).outputStream().use{if(!layer.bitmap.compress(Bitmap.CompressFormat.PNG,100,it))throw IllegalStateException("Failed to write layer asset $index")}
            val originalPath=layer.originalBitmap?.let{original->val path="assets/original_$index.png";File(folder,path).outputStream().use{if(!original.compress(Bitmap.CompressFormat.PNG,100,it))throw IllegalStateException("Failed to write original layer $index")};path}
            val operationJson=if(layer.operations.isNotEmpty())NonDestructiveEditStack(layer.originalBitmap?:layer.bitmap,layer.operations).toJson()else null
            val maskPath=layer.mask?.let{mask->val path="assets/mask_$index.png";File(folder,path).outputStream().use{if(!mask.compress(Bitmap.CompressFormat.PNG,100,it))throw IllegalStateException("Failed to write mask $index")};path}
            layerArray.put(JSONObject().put("id",layer.id).put("name",layer.name).put("visible",layer.visible).put("opacity",layer.opacity).put("blendMode",layer.blendMode.name).put("locked",layer.locked).put("alphaLocked",layer.alphaLocked).put("groupId",layer.groupId).put("groupName",layer.groupName).put("groupOpacity",layer.groupOpacity).put("groupBlendMode",layer.groupBlendMode?.name).put("clipToBelow",layer.clipToBelow).put("asset",assetPath).put("originalAsset",originalPath).put("operations",operationJson).put("maskAsset",maskPath))
        }
        val manifest=JSONObject().put("schemaVersion",4).put("projectId",id).put("name",projectName.trim().ifBlank{"Untitled Project"})
            .put("canvasWidth",canvasWidth).put("canvasHeight",canvasHeight).put("createdAt",System.currentTimeMillis()).put("updatedAt",System.currentTimeMillis())
            .put("selectedLayerId",selectedLayerId).put("layers",layerArray)
        writeManifestAtomically(folder,manifest)
        return folder
    }

    fun loadEditableProject(projectFolder:File):LoadedStudioProject? = runCatching {
        val manifest=readManifest(projectFolder)
        requireSupportedProjectFolder(projectFolder)
        val schema=manifest.optInt("schemaVersion",1)
        require(schema in 1..4) { "Unsupported project schema: $schema" }
        val name=manifest.optString("name",projectFolder.name)
        val width=manifest.optInt("canvasWidth",1).coerceAtLeast(1)
        val height=manifest.optInt("canvasHeight",1).coerceAtLeast(1)
        val array=manifest.optJSONArray("layers")
        val layers=mutableListOf<EditorLayer>()
        if(array!=null)for(i in 0 until array.length()){
            val item=array.optJSONObject(i)?:continue
            val asset=item.optString("asset",manifest.optString("snapshotAsset",manifest.optString("sourceAsset","")))
            if(asset.isBlank())continue
            val bitmap=decodeAsset(projectFolder,asset)?:continue
            val mode=runCatching{EditorBlendMode.valueOf(item.optString("blendMode","NORMAL"))}.getOrDefault(EditorBlendMode.NORMAL)
            val mask=item.optString("maskAsset").takeIf{it.isNotBlank()}?.let{decodeAsset(projectFolder,it)}
            val original=item.optString("originalAsset").takeIf{it.isNotBlank()}?.let{decodeAsset(projectFolder,it)}
            val operations=item.optJSONArray("operations")?.let{json->original?.let{NonDestructiveEditStack.fromJson(it,json).operations}}?:emptyList()
            layers+=EditorLayer(id=item.optString("id",UUID.randomUUID().toString()),name=item.optString("name","Layer ${i+1}"),bitmap=bitmap,
                visible=item.optBoolean("visible",true),opacity=item.optDouble("opacity",1.0).toFloat().coerceIn(0f,1f),blendMode=mode,
                locked=item.optBoolean("locked",false),alphaLocked=item.optBoolean("alphaLocked",false),
                groupId=item.optString("groupId").takeIf{it.isNotBlank()},groupName=item.optString("groupName").takeIf{it.isNotBlank()},
                groupOpacity=item.optDouble("groupOpacity",1.0).toFloat().coerceIn(0f,1f),groupBlendMode=item.optString("groupBlendMode").takeIf{it.isNotBlank()}?.let{runCatching{EditorBlendMode.valueOf(it)}.getOrNull()},clipToBelow=item.optBoolean("clipToBelow",false),
                mask=mask,originalBitmap=original,operations=operations)
        }
        if(layers.isEmpty()){val snapshot=loadSnapshot(projectFolder)?:return null;layers+=EditorLayer(name=name,bitmap=snapshot)}
        LoadedStudioProject(name,width,height,layers,manifest.optString("selectedLayerId").takeIf{it.isNotBlank()})
    }.getOrNull()

    /** Persist an original source and replayable edit operations separately from rendered layer snapshots. */
    fun saveOperationProject(source:Bitmap, operations:org.json.JSONArray, projectName:String):File {
        val id=UUID.randomUUID().toString()
        val folder=File(root,id).apply{mkdirs()}
        val assets=File(folder,"assets").apply{mkdirs()}
        val sourcePath="assets/original.png"
        File(folder,sourcePath).outputStream().use{if(!source.compress(Bitmap.CompressFormat.PNG,100,it))throw IllegalStateException("Failed to write original asset")}
        writeTextAtomically(File(folder,"operations.json"),operations.toString(2))
        val manifest=JSONObject().put("schemaVersion",3).put("projectId",id)
            .put("name",projectName.trim().ifBlank{"Untitled Project"})
            .put("canvasWidth",source.width).put("canvasHeight",source.height)
            .put("createdAt",System.currentTimeMillis()).put("updatedAt",System.currentTimeMillis())
            .put("originalAsset",sourcePath).put("operationsAsset","operations.json")
        writeManifestAtomically(folder,manifest)
        return folder
    }

    data class LoadedOperationProject(val name:String,val original:Bitmap,val editStack:com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack)

    fun loadOperationProject(projectFolder:File):LoadedOperationProject? {
        val manifest=readManifest(projectFolder)
        requireSupportedProjectFolder(projectFolder)
        val sourcePath=manifest.optString("originalAsset")
        if(sourcePath.isBlank()) return null
        val original=decodeAsset(projectFolder,sourcePath) ?: return null
        val operationsPath=manifest.optString("operationsAsset","operations.json")
        val operationsFile=resolveProjectAsset(projectFolder,operationsPath)
        val operations=runCatching{org.json.JSONArray(operationsFile.readText())}.getOrNull() ?: org.json.JSONArray()
        return LoadedOperationProject(manifest.optString("name",projectFolder.name),original,
            com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack.fromJson(original,operations))
    }

    private fun requireSupportedProjectFolder(projectFolder:File) {
        val canonicalRoot=root.canonicalFile
        val canonicalFolder=projectFolder.canonicalFile
        require(canonicalFolder.path.startsWith(canonicalRoot.path+File.separator)) { "Project folder is outside app storage" }
        require(canonicalFolder.isDirectory) { "Project folder is missing" }
    }
    private fun resolveProjectAsset(projectFolder:File,relativePath:String):File {
        require(relativePath.isNotBlank()) { "Empty project asset path" }
        val folder=projectFolder.canonicalFile
        val candidate=File(folder,relativePath).canonicalFile
        require(candidate.path.startsWith(folder.path+File.separator)) { "Unsafe project asset path" }
        return candidate
    }
    private fun decodeAsset(projectFolder:File,relativePath:String):Bitmap? {
        val file=resolveProjectAsset(projectFolder,relativePath)
        if(!file.isFile || file.length()<=0L) return null
        return BitmapFactory.decodeFile(file.absolutePath)?.takeIf{it.width>0&&it.height>0}
    }

    private fun writeManifestAtomically(folder:File,manifest:JSONObject) = writeTextAtomically(File(folder,"project.json"),manifest.toString(2))
    private fun writeTextAtomically(target:File,text:String) {
        val temp=File(target.parentFile,target.name+".tmp")
        val backup=File(target.parentFile,target.name+".bak")
        temp.writeText(text)
        backup.delete()
        if(target.exists()&&!target.renameTo(backup))throw IllegalStateException("Could not backup ${target.name}")
        if(!temp.renameTo(target)){if(backup.exists())backup.renameTo(target);throw IllegalStateException("Atomic write failed for ${target.name}")}
        backup.delete()
    }
    fun listProjects(): List<File> = root.listFiles()?.filter { it.isDirectory && File(it, "project.json").isFile }?.sortedByDescending { File(it, "project.json").lastModified() } ?: emptyList()

    fun readManifest(projectFolder: File): JSONObject = JSONObject(File(projectFolder, "project.json").readText())

    fun renameProject(projectFolder:File,newName:String):Boolean {
        val trimmed=newName.trim();if(trimmed.isBlank())return false
        return runCatching{val manifest=readManifest(projectFolder);manifest.put("name",trimmed).put("updatedAt",System.currentTimeMillis());writeManifestAtomically(projectFolder,manifest);true}.getOrDefault(false)
    }
    fun duplicateProject(projectFolder:File,newName:String?=null):File? {
        return runCatching{
            val sourceManifest=readManifest(projectFolder);val id=UUID.randomUUID().toString();val target=File(root,id)
            projectFolder.copyRecursively(target,overwrite=false)
            val manifest=readManifest(target);manifest.put("projectId",id).put("name",newName?.trim()?.takeIf{it.isNotBlank()}?:("${sourceManifest.optString("name","Project")} Copy")).put("createdAt",System.currentTimeMillis()).put("updatedAt",System.currentTimeMillis())
            writeManifestAtomically(target,manifest);target
        }.getOrNull()
    }
    fun deleteProject(projectId: String): Boolean = File(root, projectId).deleteRecursively()
}
