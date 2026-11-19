package com.winsmediatama.winsphotostudio.core.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Base64

/** Composite a solid, gradient or imported photo background behind a transparency-bearing source. */
object BackgroundReplacementProcessor {
    fun apply(source:Bitmap,mode:String,startColor:Int,endColor:Int,photoPngBase64:String=""):Bitmap {
        require(mode in setOf("solid","gradient","photo")) { "Unsupported background mode" }
        val output=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(output)
        if(mode=="photo") {
            require(photoPngBase64.isNotBlank()&&photoPngBase64.length<=24_000_000) { "Photo background data is missing or too large" }
            val bytes=Base64.decode(photoPngBase64,Base64.DEFAULT)
            val background=BitmapFactory.decodeByteArray(bytes,0,bytes.size)?:throw IllegalArgumentException("Invalid photo background")
            try {
                val scale=maxOf(source.width.toFloat()/background.width,source.height.toFloat()/background.height)
                val bw=background.width*scale;val bh=background.height*scale
                val dst=android.graphics.RectF((source.width-bw)/2f,(source.height-bh)/2f,(source.width+bw)/2f,(source.height+bh)/2f)
                canvas.drawBitmap(background,null,dst,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            } finally { background.recycle() }
        } else if(mode=="solid") {
            canvas.drawColor(startColor)
        } else {
            val paint=Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader=LinearGradient(0f,0f,source.width.toFloat(),source.height.toFloat(),startColor,endColor,Shader.TileMode.CLAMP)
            canvas.drawRect(0f,0f,source.width.toFloat(),source.height.toFloat(),paint)
            paint.shader=null
        }
        canvas.drawBitmap(source,0f,0f,Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return output
    }
}
