package com.winsmediatama.winsphotostudio.core.selection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Metadata for a user-saved selection mask. The mask asset is stored as a separate PNG. */
data class SavedSelection(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val createdAt: Long
)

/**
 * App-private saved selection storage. Masks are persisted as grayscale/alpha-compatible PNG
 * assets so they can be reloaded independently from a project and never overwrite source photos.
 */
class SavedSelectionStore(root: File) {
    private val directory = File(root, "saved-selections").apply { mkdirs() }
    private val manifest = File(directory, "selections.json")

    fun list(): List<SavedSelection> {
        val array = readManifest().optJSONArray("selections") ?: JSONArray()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: "Selection"
                add(SavedSelection(id, name, item.optInt("width", 0), item.optInt("height", 0), item.optLong("createdAt", 0L)))
            }
        }.sortedByDescending { it.createdAt }
    }

    fun save(name: String, mask: Bitmap): SavedSelection {
        require(name.trim().isNotEmpty()) { "Selection name cannot be blank" }
        require(mask.width > 0 && mask.height > 0) { "Selection mask is empty" }
        val id = UUID.randomUUID().toString()
        val asset = File(directory, "$id.png")
        asset.outputStream().use { output ->
            check(mask.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Unable to write selection mask" }
        }
        val saved = SavedSelection(id, name.trim(), mask.width, mask.height, System.currentTimeMillis())
        val entries = readManifest().optJSONArray("selections") ?: JSONArray()
        val updated = JSONArray()
        for (i in 0 until entries.length()) entries.optJSONObject(i)?.let(updated::put)
        updated.put(JSONObject().put("id", saved.id).put("name", saved.name).put("width", saved.width).put("height", saved.height).put("createdAt", saved.createdAt).put("asset", asset.name))
        writeManifest(JSONObject().put("schemaVersion", 1).put("selections", updated))
        return saved
    }

    fun load(id: String): Bitmap? {
        val item = readManifest().optJSONArray("selections")?.let { array ->
            (0 until array.length()).mapNotNull { array.optJSONObject(it) }.firstOrNull { it.optString("id") == id }
        } ?: return null
        val assetName = item.optString("asset").takeIf { it.isNotBlank() } ?: "$id.png"
        val asset = File(directory, assetName)
        if (!asset.canonicalPath.startsWith(directory.canonicalPath + File.separator)) return null
        return BitmapFactory.decodeFile(asset.absolutePath)
    }

    fun rename(id: String, newName: String): Boolean {
        if (newName.trim().isBlank()) return false
        val array = readManifest().optJSONArray("selections") ?: return false
        var found = false
        val updated = JSONArray()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (item.optString("id") == id) { item.put("name", newName.trim()); found = true }
            updated.put(item)
        }
        if (found) writeManifest(JSONObject().put("schemaVersion", 1).put("selections", updated))
        return found
    }

    fun delete(id: String): Boolean {
        val array = readManifest().optJSONArray("selections") ?: return false
        var found = false
        val updated = JSONArray()
        var assetName = "$id.png"
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (item.optString("id") == id) { found = true; assetName = item.optString("asset", assetName) }
            else updated.put(item)
        }
        if (!found) return false
        writeManifest(JSONObject().put("schemaVersion", 1).put("selections", updated))
        File(directory, assetName).delete()
        return true
    }

    private fun readManifest(): JSONObject = runCatching { JSONObject(manifest.readText()) }.getOrDefault(JSONObject())
    private fun writeManifest(value: JSONObject) {
        val temp = File(directory, "selections.json.tmp")
        temp.writeText(value.toString(2))
        if (!temp.renameTo(manifest)) {
            manifest.writeText(temp.readText())
            temp.delete()
        }
    }
}
