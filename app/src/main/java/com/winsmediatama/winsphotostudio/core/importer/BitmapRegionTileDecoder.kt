package com.winsmediatama.winsphotostudio.core.importer

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import android.os.CancellationSignal
import android.net.Uri
import java.io.Closeable

/**
 * Bounded region decoder for tiled source inspection/rendering.
 * Decoder is owned by this instance and must be closed by the caller.
 */
class BitmapRegionTileDecoder private constructor(
    private val decoder:BitmapRegionDecoder,
    val sourceWidth:Int,
    val sourceHeight:Int
):Closeable {
    companion object {
        const val MAX_TILE_OUTPUT_PIXELS=4_194_304L

        fun open(resolver:ContentResolver,uri:Uri):BitmapRegionTileDecoder? {
            val input=resolver.openInputStream(uri)?:return null
            try {
                val decoder=if(Build.VERSION.SDK_INT>=31) BitmapRegionDecoder.newInstance(input) else BitmapRegionDecoder.newInstance(input,false)
                if(decoder==null)return null
                val w=decoder.width;val h=decoder.height
                if(w<=0||h<=0||w>PreviewImageDecoder.MAX_SOURCE_DIMENSION||h>PreviewImageDecoder.MAX_SOURCE_DIMENSION) {
                    decoder.recycle()
                    return null
                }
                return BitmapRegionTileDecoder(decoder,w,h)
            } finally { input.close() }
        }
    }

    fun decode(rect:Rect,sampleSize:Int=1,cancellation:CancellationSignal?=null):Bitmap {
        cancellation?.throwIfCanceled()
        require(sampleSize in 1..64){"Sample size must be between 1 and 64"}
        val region=Rect(rect)
        require(!region.isEmpty()&&region.left>=0&&region.top>=0&&region.right<=sourceWidth&&region.bottom<=sourceHeight) {
            "Tile region must lie within source bounds"
        }
        val expectedW=(region.width()+sampleSize-1)/sampleSize
        val expectedH=(region.height()+sampleSize-1)/sampleSize
        require(expectedW.toLong()*expectedH<=MAX_TILE_OUTPUT_PIXELS){"Decoded tile exceeds pixel safety limit"}
        val options=BitmapFactory.Options().apply {
            inSampleSize=sampleSize
            inPreferredConfig=Bitmap.Config.ARGB_8888
        }
        val bitmap=decoder.decodeRegion(region,options)?:throw IllegalStateException("Region decoder returned null")
        try {
            cancellation?.throwIfCanceled()
            return bitmap
        } catch(error:Throwable) {
            if(!bitmap.isRecycled)bitmap.recycle()
            throw error
        }
    }

    override fun close() { if(!decoder.isRecycled)decoder.recycle() }
}
