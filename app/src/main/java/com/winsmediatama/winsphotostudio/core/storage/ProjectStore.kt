package com.winsmediatama.winsphotostudio.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
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
        snapshot.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
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

    fun loadSnapshot(projectFolder: File): Bitmap? {
        val manifest = readManifest(projectFolder)
        val path = manifest.optString("snapshotAsset", "assets/canvas.png")
        return android.graphics.BitmapFactory.decodeFile(File(projectFolder, path).absolutePath)
    }

    fun projectName(projectFolder: File): String = readManifest(projectFolder).optString("name", projectFolder.name)

    fun saveLayerProject(layers:List<EditorLayer>,selectedLayerId:String?,canvasWidth:Int,canvasHeight:Int,projectName:String):File {
        val id=UUID.randomUUID().toString()
        val folder=File(root,id).apply{mkdirs()}
        val assets=File(folder,"assets").apply{mkdirs()}
        val layerArray=JSONArray()
        layers.forEachIndexed { index,layer ->
            val assetPath="assets/layer_${index}.png"
            File(folder,assetPath).outputStream().use { layer.bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            layerArray.put(JSONObject().put("id",layer.id).put("name",layer.name).put("visible",layer.visible).put("opacity",layer.opacity).put("blendMode",layer.blendMode.name).put("asset",assetPath).apply { layer.mask?.let { maskBitmap -> val maskPath="assets/mask_${index}.png"; File(folder,maskPath).outputStream().use { maskBitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; put("maskAsset",maskPath) } })
        }
        val manifest=JSONObject().put("schemaVersion",2).put("projectId",id).put("name",projectName.trim().ifBlank{"Untitled Project"})
            .put("canvasWidth",canvasWidth).put("canvasHeight",canvasHeight).put("createdAt",System.currentTimeMillis()).put("updatedAt",System.currentTimeMillis())
            .put("selectedLayerId",selectedLayerId).put("layers",layerArray)
        File(folder,"project.json").writeText(manifest.toString(2))
        return folder
    }

    fun loadEditableProject(projectFolder:File):LoadedStudioProject? {
        val manifest=readManifest(projectFolder)
        val name=manifest.optString("name",projectFolder.name)
        val width=manifest.optInt("canvasWidth",1).coerceAtLeast(1)
        val height=manifest.optInt("canvasHeight",1).coerceAtLeast(1)
        val array=manifest.optJSONArray("layers")
        val layers=mutableListOf<EditorLayer>()
        if(array!=null) for(i in 0 until array.length()) {
            val item=array.optJSONObject(i) ?: continue
            val asset=item.optString("asset",manifest.optString("snapshotAsset",manifest.optString("sourceAsset","")))
            if(asset.isBlank()) continue
            val bitmap=android.graphics.BitmapFactory.decodeFile(File(projectFolder,asset).absolutePath) ?: continue
            val mode=runCatching{EditorBlendMode.valueOf(item.optString("blendMode","NORMAL"))}.getOrDefault(EditorBlendMode.NORMAL)
            layers+=EditorLayer(id=item.optString("id",UUID.randomUUID().toString()),name=item.optString("name","Layer ${i+1}"),bitmap=bitmap,
                visible=item.optBoolean("visible",true),opacity=item.optDouble("opacity",1.0).toFloat().coerceIn(0f,1f),blendMode=mode,mask=item.optString("maskAsset").takeIf{it.isNotBlank()}?.let{android.graphics.BitmapFactory.decodeFile(File(projectFolder,it).absolutePath)})
        }
        if(layers.isEmpty()) {
            val snapshot=loadSnapshot(projectFolder) ?: return null
            layers+=EditorLayer(name=name,bitmap=snapshot)
        }
        return LoadedStudioProject(name,width,height,layers,manifest.optString("selectedLayerId").takeIf{it.isNotBlank()})
    }

    /** Persist an original source and replayable edit operations separately from rendered layer snapshots. */
    fun saveOperationProject(source:Bitmap, operations:org.json.JSONArray, projectName:String):File {
        val id=UUID.randomUUID().toString()
        val folder=File(root,id).apply{mkdirs()}
        val assets=File(folder,"assets").apply{mkdirs()}
        val sourcePath="assets/original.png"
        File(folder,sourcePath).outputStream().use{source.compress(Bitmap.CompressFormat.PNG,100,it)}
        File(folder,"operations.json").writeText(operations.toString(2))
        val manifest=JSONObject().put("schemaVersion",3).put("projectId",id)
            .put("name",projectName.trim().ifBlank{"Untitled Project"})
            .put("canvasWidth",source.width).put("canvasHeight",source.height)
            .put("createdAt",System.currentTimeMillis()).put("updatedAt",System.currentTimeMillis())
            .put("originalAsset",sourcePath).put("operationsAsset","operations.json")
        File(folder,"project.json").writeText(manifest.toString(2))
        return folder
    }

    data class LoadedOperationProject(val name:String,val original:Bitmap,val editStack:com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack)

    fun loadOperationProject(projectFolder:File):LoadedOperationProject? {
        val manifest=readManifest(projectFolder)
        val sourcePath=manifest.optString("originalAsset")
        if(sourcePath.isBlank()) return null
        val original=android.graphics.BitmapFactory.decodeFile(File(projectFolder,sourcePath).absolutePath) ?: return null
        val operationsPath=manifest.optString("operationsAsset","operations.json")
        val operations=runCatching{org.json.JSONArray(File(projectFolder,operationsPath).readText())}.getOrNull() ?: org.json.JSONArray()
        return LoadedOperationProject(manifest.optString("name",projectFolder.name),original,
            com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack.fromJson(original,operations))
    }

    fun listProjects(): List<File> = root.listFiles()?.filter { it.isDirectory && File(it, "project.json").isFile }?.sortedByDescending { File(it, "project.json").lastModified() } ?: emptyList()

    fun readManifest(projectFolder: File): JSONObject = JSONObject(File(projectFolder, "project.json").readText())

    fun deleteProject(projectId: String): Boolean = File(root, projectId).deleteRecursively()
}
