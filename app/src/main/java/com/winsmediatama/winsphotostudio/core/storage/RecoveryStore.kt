package com.winsmediatama.winsphotostudio.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.editor.EditorBlendMode
import com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack

/** Atomic recovery snapshots. Stored only in app-private files; source SAF files are never changed. */
class RecoveryStore(context:Context) {
    private val root=File(context.filesDir,"studio-recovery").apply{mkdirs()}
    private val manifest=File(root,"recovery.json")
    data class Recovered(val canvasWidth:Int,val canvasHeight:Int,val layers:List<EditorLayer>,val selectedLayerId:String?)

    @Synchronized fun save(layers:List<EditorLayer>,selectedLayerId:String?,canvasWidth:Int,canvasHeight:Int) {
        require(layers.isNotEmpty()){"Cannot autosave an empty layer stack"}
        val staging=File(root,"snapshot.tmp").apply{deleteRecursively();mkdirs()}
        File(staging,"assets").mkdirs()
        val entries=JSONArray()
        layers.forEachIndexed{index,layer->
            val asset="assets/layer_$index.png"
            File(staging,asset).outputStream().use{if(!layer.bitmap.compress(Bitmap.CompressFormat.PNG,100,it))throw IllegalStateException("Failed to write recovery layer $index")}
            val originalAsset=layer.originalBitmap?.let{original->val path="assets/original_$index.png";File(staging,path).outputStream().use{if(!original.compress(Bitmap.CompressFormat.PNG,100,it))throw IllegalStateException("Failed to write recovery original $index")};path}
            val operations=if(layer.operations.isNotEmpty())NonDestructiveEditStack(layer.originalBitmap?:layer.bitmap,layer.operations).toJson()else null
            val maskAsset=layer.mask?.let{mask->val path="assets/mask_$index.png";File(staging,path).outputStream().use{if(!mask.compress(Bitmap.CompressFormat.PNG,100,it))throw IllegalStateException("Failed to write recovery mask $index")};path}
            entries.put(JSONObject().put("id",layer.id).put("name",layer.name).put("visible",layer.visible).put("opacity",layer.opacity).put("blendMode",layer.blendMode.name).put("locked",layer.locked).put("alphaLocked",layer.alphaLocked).put("groupId",layer.groupId).put("groupName",layer.groupName).put("groupOpacity",layer.groupOpacity).put("clipToBelow",layer.clipToBelow).put("asset",asset).put("originalAsset",originalAsset).put("operations",operations).put("maskAsset",maskAsset))
        }
        val json=JSONObject().put("schemaVersion",3).put("canvasWidth",canvasWidth).put("canvasHeight",canvasHeight).put("selectedLayerId",selectedLayerId).put("updatedAt",System.currentTimeMillis()).put("layers",entries)
        File(staging,"recovery.json").writeText(json.toString(2))
        val target=File(root,"snapshot");val backup=File(root,"snapshot.bak")
        backup.deleteRecursively()
        if(target.exists()&&!target.renameTo(backup))throw IllegalStateException("Could not stage previous recovery snapshot")
        if(!staging.renameTo(target)){if(backup.exists())backup.renameTo(target);throw IllegalStateException("Could not atomically replace recovery snapshot")}
        val manifestTemp=File(root,"recovery.tmp")
        manifestTemp.writeText(JSONObject().put("snapshot","snapshot/recovery.json").put("updatedAt",System.currentTimeMillis()).toString())
        val manifestBackup=File(root,"recovery.bak")
        manifestBackup.delete()
        if(manifest.exists()&&!manifest.renameTo(manifestBackup))throw IllegalStateException("Could not stage recovery manifest")
        if(!manifestTemp.renameTo(manifest)){if(manifestBackup.exists())manifestBackup.renameTo(manifest);throw IllegalStateException("Recovery manifest write failed")}
        manifestBackup.delete();backup.deleteRecursively()
    }
    fun exists():Boolean=manifest.isFile
    fun recover():Recovered?=runCatching{
        val snapshot=File(root,JSONObject(manifest.readText()).optString("snapshot"))
        val json=JSONObject(snapshot.readText());val layerArray=json.optJSONArray("layers")?:JSONArray();val folder=snapshot.parentFile?:root
        val layers=mutableListOf<EditorLayer>()
        for(i in 0 until layerArray.length()){
            val item=layerArray.optJSONObject(i)?:continue
            val bitmap=BitmapFactory.decodeFile(File(folder,item.optString("asset")).absolutePath)?:continue
            val mask=item.optString("maskAsset").takeIf{it.isNotBlank()}?.let{BitmapFactory.decodeFile(File(folder,it).absolutePath)}
            val original=item.optString("originalAsset").takeIf{it.isNotBlank()}?.let{BitmapFactory.decodeFile(File(folder,it).absolutePath)}
            val operations=item.optJSONArray("operations")?.let{array->original?.let{NonDestructiveEditStack.fromJson(it,array).operations}}?:emptyList()
            val mode=runCatching{EditorBlendMode.valueOf(item.optString("blendMode","NORMAL"))}.getOrDefault(EditorBlendMode.NORMAL)
            layers+=EditorLayer(id=item.optString("id"),name=item.optString("name","Layer ${i+1}"),bitmap=bitmap,visible=item.optBoolean("visible",true),opacity=item.optDouble("opacity",1.0).toFloat().coerceIn(0f,1f),blendMode=mode,mask=mask,locked=item.optBoolean("locked",false),alphaLocked=item.optBoolean("alphaLocked",false),groupId=item.optString("groupId").takeIf{it.isNotBlank()},groupName=item.optString("groupName").takeIf{it.isNotBlank()},groupOpacity=item.optDouble("groupOpacity",1.0).toFloat().coerceIn(0f,1f),clipToBelow=item.optBoolean("clipToBelow",false),originalBitmap=original,operations=operations)
        }
        if(layers.isEmpty())return@runCatching null
        Recovered(json.optInt("canvasWidth",layers.first().bitmap.width),json.optInt("canvasHeight",layers.first().bitmap.height),layers,json.optString("selectedLayerId").takeIf{it.isNotBlank()})
    }.getOrNull()
    fun clear():Boolean=root.deleteRecursively().also{root.mkdirs()}
}
