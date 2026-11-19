package com.winsmediatama.winsphotostudio.core.storage

import android.content.Context
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
        context.contentResolver.openInputStream(uri)?.use { input -> imageFile.outputStream().use(input::copyTo) }
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

    fun listProjects(): List<File> = root.listFiles()?.filter { it.isDirectory && File(it, "project.json").isFile }?.sortedByDescending { File(it, "project.json").lastModified() } ?: emptyList()

    fun readManifest(projectFolder: File): JSONObject = JSONObject(File(projectFolder, "project.json").readText())

    fun deleteProject(projectId: String): Boolean = File(root, projectId).deleteRecursively()
}
