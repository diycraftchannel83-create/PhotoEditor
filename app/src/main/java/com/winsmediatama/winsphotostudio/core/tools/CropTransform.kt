package com.winsmediatama.winsphotostudio.core.tools

/** Crop rectangle in normalized source-image coordinates (0..1). */
data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun isValid(): Boolean = left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f && left < right && top < bottom
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    companion object { val FULL = CropRect(0f, 0f, 1f, 1f) }
}

data class ImageTransform(
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationDegrees: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)

enum class CropAspectRatio(val width: Int, val height: Int) {
    FREE(0, 0), SQUARE(1, 1), PORTRAIT_4_5(4, 5), PORTRAIT_3_4(3, 4), PHOTO_5_7(5, 7), LANDSCAPE_4_3(4, 3), WIDE_16_9(16, 9), STORY_9_16(9, 16)
}
