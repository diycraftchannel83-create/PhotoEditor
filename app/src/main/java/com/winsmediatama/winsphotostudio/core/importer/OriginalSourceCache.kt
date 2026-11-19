package com.winsmediatama.winsphotostudio.core.importer

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.IOException

/**
 * Disk-backed original source cache for later full-resolution replay.
 * The original is copied without decoding and can be reopened for bounded full-resolution decode.
 * The caller owns and must recycle every decoded bitmap returned by [decode].
 */
class OriginalSourceCache(private val cacheDirectory: File) {
    data class CachedSource(val file: File, val width: Int, val height: Int, val byteCount: Long)

    fun cache(resolver: ContentResolver, uri: Uri, stableId: String): CachedSource {
        require(stableId.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "Invalid cache identifier" }
        if (!cacheDirectory.exists() && !cacheDirectory.mkdirs()) throw IOException("Cannot create source cache")
        val target = File(cacheDirectory, "$stableId.original")
        val temp = File(cacheDirectory, "$stableId.tmp")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IOException("Unable to read image source")
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Invalid source image dimensions" }
        require(bounds.outWidth <= PreviewImageDecoder.MAX_SOURCE_DIMENSION && bounds.outHeight <= PreviewImageDecoder.MAX_SOURCE_DIMENSION) { "Source image dimensions exceed safety limit" }
        temp.delete()
        resolver.openInputStream(uri)?.use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
            ?: throw IOException("Unable to copy image source")
        val backup=File(cacheDirectory,"$stableId.backup")
        backup.delete()
        if(target.exists()&&!target.renameTo(backup)) {
            temp.delete()
            throw IOException("Could not stage previous cached original")
        }
        if(!temp.renameTo(target)) {
            temp.delete()
            if(backup.exists())backup.renameTo(target)
            throw IOException("Could not finalize cached original")
        }
        backup.delete()
        return CachedSource(target, bounds.outWidth, bounds.outHeight, target.length())
    }

    fun decode(source: CachedSource, maxPixels: Long = 24_000_000L): Bitmap {
        require(source.file.isFile && source.file.length() > 0L) { "Cached source missing" }
        require(maxPixels in 1L..24_000_000L) { "Decode pixel limit outside safety bounds" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.file.absolutePath, bounds)
        require(bounds.outWidth == source.width && bounds.outHeight == source.height) { "Cached source metadata mismatch" }
        val sample=calculateSampleSizeForPixelLimit(bounds.outWidth,bounds.outHeight,maxPixels)
        val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        return BitmapFactory.decodeFile(source.file.absolutePath, options) ?: throw IOException("Decode cached original failed")
    }

    companion object {
        fun calculateSampleSizeForPixelLimit(width:Int,height:Int,maxPixels:Long):Int {
            require(width>0&&height>0) { "Invalid image bounds" }
            require(maxPixels in 1L..24_000_000L) { "Pixel limit outside safety bounds" }
            var sample=1
            while ((width.toLong()/sample)*(height.toLong()/sample)>maxPixels) {
                if(sample>=1 shl 30) throw IllegalArgumentException("Unable to calculate safe sample")
                sample*=2
            }
            return sample
        }
    }

    fun delete(stableId: String): Boolean {
        require(stableId.matches(Regex("[A-Za-z0-9_-]{1,80}"))) { "Invalid cache identifier" }
        return File(cacheDirectory, "$stableId.original").delete()
    }

    fun clear(): Boolean = cacheDirectory.deleteRecursively().also { cacheDirectory.mkdirs() }
}
