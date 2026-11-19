package com.winsmediatama.winsphotostudio.core.editor

import android.graphics.Bitmap
import java.util.UUID
import com.winsmediatama.winsphotostudio.core.history.EditOperation

enum class EditorBlendMode { NORMAL, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN, ADD, DIFFERENCE }

data class EditorLayer(
    val id:String=UUID.randomUUID().toString(),
    val name:String,
    val bitmap:Bitmap,
    val visible:Boolean=true,
    val opacity:Float=1f,
    val blendMode:EditorBlendMode=EditorBlendMode.NORMAL,
    /** Optional grayscale layer mask: white reveals and black hides. */
    val mask:Bitmap?=null,
    val locked:Boolean=false,
    val alphaLocked:Boolean=false,
    val groupId:String?=null,
    val groupName:String?=null,
    val groupOpacity:Float=1f,
    val clipToBelow:Boolean=false,
    val originalBitmap:Bitmap?=null,
    val operations:List<EditOperation> = emptyList()
)
