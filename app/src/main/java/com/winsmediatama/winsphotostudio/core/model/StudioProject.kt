package com.winsmediatama.winsphotostudio.core.model

import java.util.UUID

/** Serializable project metadata. Source pixels remain in referenced asset files. */
data class StudioProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Project",
    val canvasWidth: Int,
    val canvasHeight: Int,
    val layers: List<StudioLayer> = listOf(StudioLayer(name = "Background", isBackground = true)),
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val schemaVersion: Int = 1
)

data class StudioLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val opacity: Float = 1f,
    val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    val isBackground: Boolean = false,
    val sourceAsset: String? = null,
    val maskAsset: String? = null,
    val adjustments: List<AdjustmentData> = emptyList()
)

enum class LayerBlendMode { NORMAL, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN, COLOR, LUMINOSITY }

data class AdjustmentData(
    val type: AdjustmentType,
    val enabled: Boolean = true,
    val values: Map<String, Float> = emptyMap()
)

enum class AdjustmentType { BRIGHTNESS, CONTRAST, EXPOSURE, SATURATION, TEMPERATURE, TINT, HIGHLIGHTS, SHADOWS, WHITES, BLACKS, VIBRANCE, CLARITY, SHARPEN }
