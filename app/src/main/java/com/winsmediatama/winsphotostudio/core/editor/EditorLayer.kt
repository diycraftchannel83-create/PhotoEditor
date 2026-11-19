package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import java.util.UUID

data class EditorLayer(
    val id:String=UUID.randomUUID().toString(),
    val name:String,
    val bitmap:Bitmap,
    val visible:Boolean=true,
    val opacity:Float=1f
)
