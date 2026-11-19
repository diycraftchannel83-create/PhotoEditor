package com.winsmediatama.winsphotostudio.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Local project persistence. Assets are copied into the app-owned project folder; originals are untouched. */
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

    fun listProjects(): List<File> = root.listFiles()?.filter { it.isDirectory && File(it, "project.json").isFile }?.sortedByDescending { File(it, "project.json").lastModified() } ?: emptyList()

    fun readManifest(projectFolder: File): JSONObject = JSONObject(File(projectFolder, "project.json").readText())

    fun deleteProject(projectId: String): Boolean = File(root, projectId).deleteRecursively()
}
