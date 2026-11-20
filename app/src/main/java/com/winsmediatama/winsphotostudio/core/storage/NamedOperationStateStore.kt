package com.winsmediatama.winsphotostudio.core.storage

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import com.winsmediatama.winsphotostudio.core.history.EditOperation
import com.winsmediatama.winsphotostudio.core.history.NonDestructiveEditStack
import java.io.File

/** Persistent named non-destructive operation states, scoped to editor layer IDs. */
class NamedOperationStateStore(context:Context) {
    private val root=File(context.filesDir,"wins-photo-operation-states").apply{mkdirs()}
    private val maxStates=50
    private fun fileFor(layerId:String):File {
        require(layerId.isNotBlank()&&layerId.length<=256){"Invalid layer ID"}
        val safe=layerId.map{if(it.isLetterOrDigit()||it=='-'||it=='_')it else '_'}
            .joinToString("").take(80)
        val file=File(root,"${safe}-${layerId.hashCode().toUInt().toString(16)}.json").canonicalFile
        require(file.path.startsWith(root.canonicalPath+File.separator)){"Unsafe state path"}
        return file
    }
    @Synchronized fun save(layerId:String,name:String,operations:List<EditOperation>):Boolean {
        val key=name.trim()
        if(key.isBlank()||key.length>80||operations.size>200)return false
        val file=fileFor(layerId)
        val existing=read(file)
        if(existing.length()>=maxStates&&!existing.has(key))return false
        existing.put(key,JSONArray().also{array->operations.forEach{array.put(it.toJson())}})
        val document=JSONObject().put("schemaVersion",1).put("layerId",layerId).put("states",existing)
        val temp=File(root,file.name+".tmp")
        try {
            temp.writeText(document.toString(2))
            if(file.exists()) {
                val backup=File(root,file.name+".bak")
                if(backup.exists())backup.delete()
                if(!file.renameTo(backup))throw IllegalStateException("Unable to stage previous state file")
                if(!temp.renameTo(file)){backup.renameTo(file);throw IllegalStateException("Unable to install named state file")}
                backup.delete()
            } else if(!temp.renameTo(file))throw IllegalStateException("Unable to install named state file")
            return true
        } finally { temp.delete() }
    }
    @Synchronized fun names(layerId:String):List<String> = read(fileFor(layerId)).keys().asSequence().toList().sorted()
    @Synchronized fun load(layerId:String,name:String,source:Bitmap):List<EditOperation>? {
        val array=read(fileFor(layerId)).optJSONArray(name)?:return null
        return runCatching{NonDestructiveEditStack.fromJson(source,array).operations}.getOrNull()
    }
    @Synchronized fun remove(layerId:String,name:String):Boolean {
        val file=fileFor(layerId);val states=read(file)
        if(!states.has(name))return false
        states.remove(name)
        if(states.length()==0){file.delete();return true}
        val temp=File(root,file.name+".tmp")
        try {temp.writeText(JSONObject().put("schemaVersion",1).put("layerId",layerId).put("states",states).toString(2));if(!temp.renameTo(file)){temp.delete();return false};return true}
        finally{temp.delete()}
    }
    private fun read(file:File):JSONObject {
        if(!file.isFile)return JSONObject()
        return runCatching{
            val document=JSONObject(file.readText())
            require(document.optInt("schemaVersion")==1)
            require(document.optString("layerId")==file.name.substringBeforeLast('-') || document.optString("layerId").isNotBlank())
            val states=document.optJSONObject("states")?:JSONObject()
            require(states.length()<=maxStates)
            states
        }.getOrDefault(JSONObject())
    }
}
