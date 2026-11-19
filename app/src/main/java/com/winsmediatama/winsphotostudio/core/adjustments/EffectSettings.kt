package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap

enum class PhotoEffectKind { BLUR, MOTION, VIGNETTE, GRAIN }

data class PhotoEffectSettings(
    val kind: PhotoEffectKind = PhotoEffectKind.VIGNETTE,
    val strength: Float = .5f,
    val angleDegrees: Float = 45f
) {
    fun valid() = strength.isFinite() && angleDegrees.isFinite() && strength in 0f..1f
    fun apply(source: Bitmap): Bitmap = when(kind) {
        PhotoEffectKind.BLUR -> EffectProcessor.gaussianBlur(source, (strength * 24).toInt())
        PhotoEffectKind.MOTION -> if(strength <= 0f) source.copy(Bitmap.Config.ARGB_8888,true) else EffectProcessor.motionBlur(source,(strength*24).toInt().coerceAtLeast(1),angleDegrees)
        PhotoEffectKind.VIGNETTE -> EffectProcessor.vignette(source,strength)
        PhotoEffectKind.GRAIN -> EffectProcessor.grain(source,strength,seed=101)
    }
}
