package com.winsmediatama.winsphotostudio.core.tools

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** UI-independent normalized point; values are relative to the edited image. */
data class RetouchPoint(val x: Float, val y: Float) {
    fun valid(): Boolean = x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f
}

/** Serializable retouch tool mode. */
enum class RetouchMode(val label: String) {
    BLUR("Blur"), PIXELATE("Pixelate"), CLONE("Clone"), SPOT_HEAL("Spot heal"),
    DODGE("Dodge"), BURN("Burn"), SMUDGE("Smudge")
}

/** Replayable normalized stroke. Brush size is stored in source-image pixels. */
data class RetouchStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<RetouchPoint>,
    val size: Float,
    val mode: RetouchMode
) {
    fun valid(): Boolean = id.isNotBlank() && points.isNotEmpty() && points.all { it.valid() } &&
        size.isFinite() && size in 1f..10000f

    fun toJson(): JSONObject = JSONObject().put("schema", 1).put("id", id)
        .put("mode", mode.name).put("size", size)
        .put("points", JSONArray().also { array -> points.forEach { array.put(JSONObject().put("x", it.x.toDouble()).put("y", it.y.toDouble())) } })

    companion object {
        fun fromJson(json: JSONObject): RetouchStroke? = runCatching {
            if (json.optInt("schema", 1) != 1) return null
            val mode = RetouchMode.valueOf(json.getString("mode"))
            val pointsJson = json.getJSONArray("points")
            val points = (0 until pointsJson.length()).map { index ->
                val p = pointsJson.getJSONObject(index)
                RetouchPoint(p.getDouble("x").toFloat(), p.getDouble("y").toFloat())
            }
            RetouchStroke(json.optString("id").ifBlank { UUID.randomUUID().toString() }, points,
                json.getDouble("size").toFloat(), mode).takeIf { it.valid() }
        }.getOrNull()
    }
}

object RetouchStrokeCodec {
    fun encode(strokes: List<RetouchStroke>): String = JSONArray().also { array ->
        strokes.forEach { if (it.valid()) array.put(it.toJson()) }
    }.toString()

    fun decode(json: String): List<RetouchStroke> = runCatching {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { RetouchStroke.fromJson(array.optJSONObject(it) ?: JSONObject()) }
    }.getOrDefault(emptyList())
}
