package com.winsmediatama.winsphotostudio.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.editor.EditorBlendMode

/** Atomic recovery snapshots. Stored only in app-private files; source SAF files are never changed. */
class RecoveryStore(context:Context) {
    private val root=File(context.filesDir,"studio-recovery").apply{mkdirs()}
    private val manifest=File(root,"recovery.json")
    data class Recovered(val canvasWidth:Int,val canvasHeight:Int,val layers:List<EditorLayer>,val selectedLayerId:String?)

    fun save(layers:List<EditorLayer>,selectedLayerId:String?,canvasWidth:Int,canvasHeight:Int) {
        require(layers.isNotEmpty()){"Cannot autosave an empty layer stack"}
        val staging=File(root,"snapshot.tmp").apply{deleteRecursively();mkdirs()}
        val assetDir=File(staging,"assets").apply{mkdirs()}
        val entries=JSONArray()
        layers.forEachIndexed{index,layer->
            val asset="assets/layer_$index.png"
            File(staging,asset).outputStream().use{layer.bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            val maskAsset=layer.mask?.let{mask->val path="assets/mask_$index.png";File(staging,path).outputStream().use{mask.compress(Bitmap.CompressFormat.PNG,100,it)};path}
            entries.put(JSONObject().put("id",layer.id).put("name",layer.name).put("visible",layer.visible).put("opacity",layer.opacity).put("blendMode",layer.blendMode.name).put("asset",asset).apply{if(maskAsset!=null)put("maskAsset",maskAsset)})
        }
        val json=JSONObject().put("schemaVersion",1).put("canvasWidth",canvasWidth).put("canvasHeight",canvasHeight).put("selectedLayerId",selectedLayerId).put("updatedAt",System.currentTimeMillis()).put("layers",entries)
        File(staging,"recovery.json").writeText(json.toString(2))
        val target=File(root,"snapshot")
        if(target.exists())target.deleteRecursively()
        if(!staging.renameTo(target))throw IllegalStateException("Could not atomically replace recovery snapshot")
        manifest.writeText(JSONObject().put("snapshot","snapshot/recovery.json").put("updatedAt",System.currentTimeMillis()).toString())
    }
    fun exists():Boolean=manifest.isFile
    fun recover():Recovered?=runCatching{
        val snapshot=File(root,JSONObject(manifest.readText()).optString("snapshot"))
        val json=JSONObject(snapshot.readText());val layerArray=json.optJSONArray("layers")?:JSONArray()
        val folder=snapshot.parentFile ?: root
        val layers=mutableListOf<EditorLayer>()
        for(i in 0 until layerArray.length()){
            val item=layerArray.optJSONObject(i)?:continue
            val bitmap=BitmapFactory.decodeFile(File(folder,item.optString("asset")).absolutePath)?:continue
            val mask=item.optString("maskAsset").takeIf{it.isNotBlank()}?.let{BitmapFactory.decodeFile(File(folder,it).absolutePath)}
            val mode=runCatching{EditorBlendMode.valueOf(item.optString("blendMode","NORMAL"))}.getOrDefault(EditorBlendMode.NORMAL)
            layers+=EditorLayer(item.optString("id"),item.optString("name","Layer ${i+1}"),bitmap,item.optBoolean("visible",true),item.optDouble("opacity",1.0).toFloat().coerceIn(0f,1f),mode,mask)
        }
        if(layers.isEmpty())return@runCatching null
        Recovered(json.optInt("canvasWidth",layers.first().bitmap.width),json.optInt("canvasHeight",layers.first().bitmap.height),layers,json.optString("selectedLayerId").takeIf{it.isNotBlank()})
    }.getOrNull()
    fun clear():Boolean=root.deleteRecursively().also{root.mkdirs()}
}
