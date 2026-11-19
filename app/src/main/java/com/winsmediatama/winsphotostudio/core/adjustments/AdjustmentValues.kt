package com.winsmediatama.winsphotostudio.core.adjustments

/** Normalized adjustment parameters; renderers define the visual mapping. */
data class BasicAdjustmentValues(
    val brightness: Float = 0f, // -1..1
    val contrast: Float = 0f, // -1..1
    val exposure: Float = 0f, // EV, -5..5
    val saturation: Float = 0f, // -1..1
    val hue: Float = 0f, // -1..1, mapped to -180..180 degrees
    val temperature: Float = 0f, // -1..1
    val tint: Float = 0f, // -1..1
    val redBalance: Float = 0f, // -1..1
    val greenBalance: Float = 0f, // -1..1
    val blueBalance: Float = 0f, // -1..1
    val highlights: Float = 0f, // -1..1
    val shadows: Float = 0f, // -1..1
    val whites: Float = 0f, // -1..1
    val blacks: Float = 0f, // -1..1
    val vibrance: Float = 0f, // -1..1
    val clarity: Float = 0f, // -1..1
    val sharpen: Float = 0f // 0..1
)

/** Levels inputs and outputs for composite and individual RGB channels. */
data class LevelsChannel(
    val inputBlack: Float = 0f,
    val inputGamma: Float = 1f,
    val inputWhite: Float = 1f,
    val outputBlack: Float = 0f,
    val outputWhite: Float = 1f
) {
    fun isValid(): Boolean = inputBlack in 0f..1f && inputWhite in 0f..1f && inputBlack < inputWhite && inputGamma > 0f && outputBlack in 0f..1f && outputWhite in 0f..1f && outputBlack <= outputWhite
}

data class LevelsValues(
    val composite: LevelsChannel = LevelsChannel(),
    val red: LevelsChannel = LevelsChannel(),
    val green: LevelsChannel = LevelsChannel(),
    val blue: LevelsChannel = LevelsChannel()
)

/** Curve points are normalized 0..1 and are expected to be sorted by x during render. */
data class CurvePoint(val x: Float, val y: Float)
data class CurvesValues(
    val composite: List<CurvePoint> = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f)),
    val red: List<CurvePoint> = emptyList(),
    val green: List<CurvePoint> = emptyList(),
    val blue: List<CurvePoint> = emptyList()
)
