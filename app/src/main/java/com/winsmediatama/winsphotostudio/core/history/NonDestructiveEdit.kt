package com.winsmediatama.winsphotostudio.core.history

import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.BasicAdjustmentValues
import com.winsmediatama.winsphotostudio.core.adjustments.CurvePoint
import com.winsmediatama.winsphotostudio.core.adjustments.CurvesProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.CurvesValues
import com.winsmediatama.winsphotostudio.core.adjustments.LevelsChannel
import com.winsmediatama.winsphotostudio.core.adjustments.LevelsProcessor
import com.winsmediatama.winsphotostudio.core.adjustments.LevelsValues
import com.winsmediatama.winsphotostudio.core.adjustments.HslAdjustments
import com.winsmediatama.winsphotostudio.core.adjustments.HslColorRange
import com.winsmediatama.winsphotostudio.core.adjustments.HslRangeAdjustment
import com.winsmediatama.winsphotostudio.core.adjustments.HslProcessor\nimport com.winsmediatama.winsphotostudio.core.tools.TransformProcessor\nimport com.winsmediatama.winsphotostudio.core.tools.TransformSpec\nimport com.winsmediatama.winsphotostudio.core.adjustments.TransformProcessor

/**
 * Serializable, replayable edit stack. Operations retain parameters rather than requiring
 * the caller to overwrite the imported source bitmap. This is the foundation for live edits.
 */
sealed interface EditOperation {
    val id: String
    val enabled: Boolean
    val label: String
    fun toJson(): JSONObject

    data class BasicAdjust(
        override val id: String = java.util.UUID.randomUUID().toString(),
        override val enabled: Boolean = true,
        val values: BasicAdjustmentValues
    ) : EditOperation {
        override val label = "Basic adjustments"
        override fun toJson() = JSONObject().put("id",id).put("type","basic").put("enabled",enabled)
            .put("values",basicValuesJson(values))
    }

    data class Levels(
        override val id: String = java.util.UUID.randomUUID().toString(),
        override val enabled: Boolean = true,
        val values: LevelsValues
    ) : EditOperation {
        override val label = "Levels"
        override fun toJson() = JSONObject().put("id",id).put("type","levels").put("enabled",enabled)
            .put("values",levelsJson(values))
    }

    data class Curves(
        override val id: String = java.util.UUID.randomUUID().toString(),
        override val enabled: Boolean = true,
        val values: CurvesValues
    ) : EditOperation {
        override val label = "Curves"
        override fun toJson() = JSONObject().put("id",id).put("type","curves").put("enabled",enabled)
            .put("values",curvesJson(values))
    }

    data class Hsl(override val id:String=java.util.UUID.randomUUID().toString(),override val enabled:Boolean=true,val values:HslAdjustments):EditOperation {
        override val label="HSL / Color range"
        override fun toJson()=JSONObject().put("id",id).put("type","hsl").put("enabled",enabled).put("values",hslJson(values))
    }

    data class Transform(override val id:String=java.util.UUID.randomUUID().toString(),override val enabled:Boolean=true,val left:Float=0f,val top:Float=0f,val right:Float=1f,val bottom:Float=1f,val rotationDegrees:Float=0f,val flipHorizontal:Boolean=false,val flipVertical:Boolean=false,val outputWidth:Int=0,val outputHeight:Int=0):EditOperation {\n        override val label=\"Crop / Transform\"\n        fun valid()=TransformSpec(left,top,right,bottom,rotationDegrees,flipHorizontal,flipVertical,outputWidth,outputHeight).isValid()\n        override fun toJson()=JSONObject().put(\"id\",id).put(\"type\",\"transform\").put(\"enabled\",enabled).put(\"left\",left).put(\"top\",top).put(\"right\",right).put(\"bottom\",bottom).put(\"rotationDegrees\",rotationDegrees).put(\"flipHorizontal\",flipHorizontal).put(\"flipVertical\",flipVertical).put(\"outputWidth\",outputWidth).put(\"outputHeight\",outputHeight)\n    }\n\n    data class Crop(
        override val id: String = java.util.UUID.randomUUID().toString(),
        override val enabled: Boolean = true,
        val left: Float, val top: Float, val right: Float, val bottom: Float
    ) : EditOperation {
        override val label = "Crop"
        fun valid(): Boolean = left >= 0f && top >= 0f && right <= 1f && bottom <= 1f && left < right && top < bottom
        override fun toJson() = JSONObject().put("id",id).put("type","crop").put("enabled",enabled)
            .put("left",left).put("top",top).put("right",right).put("bottom",bottom)
    }
}

class NonDestructiveEditStack(
    private val original: Bitmap,
    operations: List<EditOperation> = emptyList()
) {
    private val mutableOperations = operations.toMutableList()
    val operations: List<EditOperation> get() = mutableOperations.toList()

    fun append(operation: EditOperation) { mutableOperations.add(operation) }
    fun insert(index:Int,operation:EditOperation){mutableOperations.add(index.coerceIn(0,mutableOperations.size),operation)}
    fun move(id:String,toIndex:Int):Boolean{val from=mutableOperations.indexOfFirst{it.id==id};if(from<0)return false;val operation=mutableOperations.removeAt(from);mutableOperations.add(toIndex.coerceIn(0,mutableOperations.size),operation);return true}
    fun replace(id: String, operation: EditOperation): Boolean {
        val index = mutableOperations.indexOfFirst { it.id == id }
        if (index < 0) return false
        mutableOperations[index] = when (operation) {
            is EditOperation.BasicAdjust -> operation.copy(id = id)
            is EditOperation.Levels -> operation.copy(id = id)
            is EditOperation.Curves -> operation.copy(id = id)
            is EditOperation.Crop -> operation.copy(id = id)
            is EditOperation.Hsl -> operation.copy(id = id)
        }
        return true
    }
    fun toggle(id: String, enabled: Boolean): Boolean {
        val index = mutableOperations.indexOfFirst { it.id == id }
        if (index < 0) return false
        mutableOperations[index] = when (val old = mutableOperations[index]) {
            is EditOperation.BasicAdjust -> old.copy(enabled = enabled)
            is EditOperation.Levels -> old.copy(enabled = enabled)
            is EditOperation.Curves -> old.copy(enabled = enabled)
            is EditOperation.Crop -> old.copy(enabled = enabled)
            is EditOperation.Hsl -> old.copy(enabled = enabled)\n            is EditOperation.Transform -> old.copy(enabled=enabled)
        }
        return true
    }
    fun remove(id: String): Boolean = mutableOperations.removeAll { it.id == id }
    fun clear() = mutableOperations.clear()

    /** Replay operations in order. Original bitmap is never modified. */
    fun render(): Bitmap {
        var current = original.copy(Bitmap.Config.ARGB_8888, true)
        mutableOperations.filter { it.enabled }.forEach { operation ->
            val next = when (operation) {
                is EditOperation.BasicAdjust -> BasicAdjustmentProcessor.apply(current, operation.values)
                is EditOperation.Levels -> LevelsProcessor.apply(current, operation.values)
                is EditOperation.Curves -> CurvesProcessor.apply(current, operation.values)
                is EditOperation.Crop -> crop(current, operation)
                is EditOperation.Hsl -> HslProcessor.apply(current,operation.values)\n                is EditOperation.Transform -> TransformProcessor.apply(current,TransformSpec(operation.left,operation.top,operation.right,operation.bottom,operation.rotationDegrees,operation.flipHorizontal,operation.flipVertical,operation.outputWidth,operation.outputHeight))
            }
            if (next !== current && !current.isRecycled) current.recycle()
            current = next
        }
        return current
    }

    fun toJson(): JSONArray = JSONArray().also { array -> mutableOperations.forEach { array.put(it.toJson()) } }

    companion object {
        fun fromJson(source: Bitmap, json: JSONArray): NonDestructiveEditStack {
            val ops = mutableListOf<EditOperation>()
            for (i in 0 until json.length()) {
                val item = json.optJSONObject(i) ?: continue
                val id = item.optString("id", java.util.UUID.randomUUID().toString())
                val enabled = item.optBoolean("enabled", true)
                val values = item.optJSONObject("values") ?: JSONObject()
                when (item.optString("type")) {
                    "basic" -> ops += EditOperation.BasicAdjust(id, enabled, basicValuesFromJson(values))
                    "levels" -> ops += EditOperation.Levels(id, enabled, levelsFromJson(values))
                    "curves" -> ops += EditOperation.Curves(id, enabled, curvesFromJson(values))
                    "crop" -> {
                        val crop = EditOperation.Crop(id, enabled,
                            item.optDouble("left", Double.NaN).toFloat(),
                            item.optDouble("top", Double.NaN).toFloat(),
                            item.optDouble("right", Double.NaN).toFloat(),
                            item.optDouble("bottom", Double.NaN).toFloat()
                        )
                        if (crop.valid()) ops += crop
                    }
                    "hsl" -> ops += EditOperation.Hsl(id,enabled,hslFromJson(values))\n                    "transform" -> { val op=EditOperation.Transform(id,enabled,item.optDouble("left",0.0).toFloat(),item.optDouble("top",0.0).toFloat(),item.optDouble("right",1.0).toFloat(),item.optDouble("bottom",1.0).toFloat(),item.optDouble("rotationDegrees",0.0).toFloat(),item.optBoolean("flipHorizontal",false),item.optBoolean("flipVertical",false),item.optInt("outputWidth",0),item.optInt("outputHeight",0));if(op.valid())ops+=op }
                }
            }
            return NonDestructiveEditStack(source, ops)
        }

        private fun crop(bitmap: Bitmap, op: EditOperation.Crop): Bitmap {
            require(op.valid()) { "Crop rectangle is invalid" }
            val left=(op.left*bitmap.width).toInt().coerceIn(0,bitmap.width-1)
            val top=(op.top*bitmap.height).toInt().coerceIn(0,bitmap.height-1)
            val right=(op.right*bitmap.width).toInt().coerceIn(left+1,bitmap.width)
            val bottom=(op.bottom*bitmap.height).toInt().coerceIn(top+1,bitmap.height)
            return Bitmap.createBitmap(bitmap,left,top,right-left,bottom-top)
        }
    }
}

private fun hslJson(v:HslAdjustments):JSONObject {
    val ranges=JSONArray()
    v.ranges.forEach{(range,value)->
        ranges.put(JSONObject().put("range",range.name).put("hueShift",value.hueShift).put("saturation",value.saturation).put("lightness",value.lightness))
    }
    return JSONObject().put("ranges",ranges)
}
private fun hslFromJson(j:JSONObject):HslAdjustments {
    val ranges=j.optJSONArray("ranges")?:return HslAdjustments()
    val values=mutableMapOf<HslColorRange,HslRangeAdjustment>()
    for(i in 0 until ranges.length()){
        val item=ranges.optJSONObject(i)?:continue
        val range=runCatching{HslColorRange.valueOf(item.optString("range"))}.getOrNull()?:continue
        values[range]=HslRangeAdjustment(item.optDouble("hueShift").toFloat(),item.optDouble("saturation").toFloat(),item.optDouble("lightness").toFloat())
    }
    return HslAdjustments(values)
}
private fun basicValuesJson(v: BasicAdjustmentValues)=JSONObject().apply {
    put("brightness",v.brightness);put("contrast",v.contrast);put("exposure",v.exposure);put("saturation",v.saturation);put("hue",v.hue)
    put("temperature",v.temperature);put("tint",v.tint);put("redBalance",v.redBalance);put("greenBalance",v.greenBalance);put("blueBalance",v.blueBalance)
    put("highlights",v.highlights);put("shadows",v.shadows);put("whites",v.whites);put("blacks",v.blacks);put("vibrance",v.vibrance);put("clarity",v.clarity);put("sharpen",v.sharpen)
}
private fun basicValuesFromJson(j:JSONObject)=BasicAdjustmentValues(
    brightness=j.optDouble("brightness",0.0).toFloat(),contrast=j.optDouble("contrast",0.0).toFloat(),exposure=j.optDouble("exposure",0.0).toFloat(),
    saturation=j.optDouble("saturation",0.0).toFloat(),hue=j.optDouble("hue",0.0).toFloat(),temperature=j.optDouble("temperature",0.0).toFloat(),
    tint=j.optDouble("tint",0.0).toFloat(),redBalance=j.optDouble("redBalance",0.0).toFloat(),greenBalance=j.optDouble("greenBalance",0.0).toFloat(),
    blueBalance=j.optDouble("blueBalance",0.0).toFloat(),highlights=j.optDouble("highlights",0.0).toFloat(),shadows=j.optDouble("shadows",0.0).toFloat(),
    whites=j.optDouble("whites",0.0).toFloat(),blacks=j.optDouble("blacks",0.0).toFloat(),vibrance=j.optDouble("vibrance",0.0).toFloat(),
    clarity=j.optDouble("clarity",0.0).toFloat(),sharpen=j.optDouble("sharpen",0.0).toFloat()
)
private fun channelJson(c:LevelsChannel)=JSONObject().put("inputBlack",c.inputBlack).put("inputGamma",c.inputGamma).put("inputWhite",c.inputWhite).put("outputBlack",c.outputBlack).put("outputWhite",c.outputWhite)
private fun channelFromJson(j:JSONObject)=LevelsChannel(j.optDouble("inputBlack",0.0).toFloat(),j.optDouble("inputGamma",1.0).toFloat(),j.optDouble("inputWhite",1.0).toFloat(),j.optDouble("outputBlack",0.0).toFloat(),j.optDouble("outputWhite",1.0).toFloat())
private fun levelsJson(v:LevelsValues)=JSONObject().put("composite",channelJson(v.composite)).put("red",channelJson(v.red)).put("green",channelJson(v.green)).put("blue",channelJson(v.blue))
private fun levelsFromJson(j:JSONObject)=LevelsValues(channelFromJson(j.optJSONObject("composite")?:JSONObject()),channelFromJson(j.optJSONObject("red")?:JSONObject()),channelFromJson(j.optJSONObject("green")?:JSONObject()),channelFromJson(j.optJSONObject("blue")?:JSONObject()))
private fun pointsJson(points:List<CurvePoint>)=JSONArray().also { a->points.forEach { a.put(JSONObject().put("x",it.x).put("y",it.y)) } }
private fun pointsFromJson(a:JSONArray?)=buildList { if(a!=null) for(i in 0 until a.length()){val p=a.optJSONObject(i)?:continue;add(CurvePoint(p.optDouble("x").toFloat(),p.optDouble("y").toFloat()))} }
private fun curvesJson(v:CurvesValues)=JSONObject().put("composite",pointsJson(v.composite)).put("red",pointsJson(v.red)).put("green",pointsJson(v.green)).put("blue",pointsJson(v.blue))
private fun curvesFromJson(j:JSONObject)=CurvesValues(pointsFromJson(j.optJSONArray("composite")).ifEmpty{listOf(CurvePoint(0f,0f),CurvePoint(1f,1f))},pointsFromJson(j.optJSONArray("red")),pointsFromJson(j.optJSONArray("green")),pointsFromJson(j.optJSONArray("blue")))
