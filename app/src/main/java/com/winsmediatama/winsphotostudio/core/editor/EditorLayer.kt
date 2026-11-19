package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import java.util.UUID

enum class EditorBlendMode { NORMAL, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN, ADD, DIFFERENCE }\n\ndata class EditorLayer(
    val id:String=UUID.randomUUID().toString(),
    val name:String,
    val bitmap:Bitmap,
    val visible:Boolean=true,
    val opacity:Float=1f,\n    val blendMode:EditorBlendMode=EditorBlendMode.NORMAL
)
