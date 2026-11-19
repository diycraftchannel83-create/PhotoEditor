package com.winsmediatama.winsphotostudio.core.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/** Memory-guarded export sizing. Upscaling preview is explicit and is not native-resolution edit replay. */
object FullResolutionExport {
    const val MAX_EXPORT_PIXELS:Long=60_000_000L

    data class Size(val width:Int,val height:Int) {
        val pixelCount:Long get()=width.toLong()*height.toLong()
        fun valid():Boolean=width>0&&height>0&&pixelCount<=MAX_EXPORT_PIXELS
    }

    fun targetSize(preview:Bitmap,sourceWidth:Int,sourceHeight:Int,useOriginalDimensions:Boolean):Size {
        val size=if(useOriginalDimensions&&sourceWidth>0&&sourceHeight>0)Size(sourceWidth,sourceHeight)else Size(preview.width,preview.height)
        require(size.valid()) { "Export dimensions exceed the 60 megapixel safety limit" }
        return size
    }

    fun render(preview:Bitmap,size:Size):Bitmap {
        require(size.valid()) { "Invalid or oversized export dimensions" }
        if(preview.width==size.width&&preview.height==size.height)return preview
        val output=Bitmap.createBitmap(size.width,size.height,Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(preview,null,android.graphics.Rect(0,0,size.width,size.height),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return output
    }
}
