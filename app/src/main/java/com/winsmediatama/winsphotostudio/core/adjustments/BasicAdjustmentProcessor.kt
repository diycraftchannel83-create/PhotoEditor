package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.pow

/** CPU reference renderer for adjustments; preview calls should run off the main thread. */
object BasicAdjustmentProcessor {
    fun apply(source: Bitmap, values: BasicAdjustmentValues): Bitmap {
        val width=source.width; val height=source.height
        val output=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val pixels=IntArray(width*height)
        source.getPixels(pixels,0,width,0,0,width,height)
        val sharpenSource=if(values.sharpen>0f) pixels.copyOf() else null
        val contrast=(1f+values.contrast.coerceIn(-1f,1f)).coerceAtLeast(0f)
        val exposure=2.0.pow(values.exposure.coerceIn(-5f,5f).toDouble()).toFloat()
        val saturation=(1f+values.saturation.coerceIn(-1f,1f)).coerceAtLeast(0f)
        val brightness=values.brightness.coerceIn(-1f,1f)*255f
        val temperature=values.temperature.coerceIn(-1f,1f)*24f
        val tint=values.tint.coerceIn(-1f,1f)*18f
        val highlights=values.highlights.coerceIn(-1f,1f)
        val shadows=values.shadows.coerceIn(-1f,1f)
        val whites=values.whites.coerceIn(-1f,1f)
        val blacks=values.blacks.coerceIn(-1f,1f)
        val vibrance=values.vibrance.coerceIn(-1f,1f)
        val clarity=values.clarity.coerceIn(-1f,1f)
        val sharpen=values.sharpen.coerceIn(0f,1f)
        val hueShift=values.hue.coerceIn(-1f,1f)*180f
        val hsv=FloatArray(3)
        for(y in 0 until height) for(x in 0 until width) {
            val i=y*width+x; val p=pixels[i]; val a=p ushr 24
            var r=((p shr 16) and 255).toFloat()
            var g=((p shr 8) and 255).toFloat()
            var b=(p and 255).toFloat()
            r=((r-127.5f)*contrast+127.5f+brightness)*exposure+temperature
            g=((g-127.5f)*contrast+127.5f+brightness)*exposure+tint
            b=((b-127.5f)*contrast+127.5f+brightness)*exposure-temperature
            var lum=(0.2126f*r+0.7152f*g+0.0722f*b).coerceIn(0f,255f)
            val shadowWeight=(1f-lum/255f).pow(2f)
            val highlightWeight=(lum/255f).pow(2f)
            val midWeight=1f-abs(lum/127.5f-1f)
            val tone=shadows*shadowWeight*65f+highlights*highlightWeight*65f+blacks*shadowWeight*35f+whites*highlightWeight*35f
            r+=tone; g+=tone; b+=tone
            lum=(0.2126f*r+0.7152f*g+0.0722f*b).coerceIn(0f,255f)
            val gray=0.2126f*r+0.7152f*g+0.0722f*b
            val maxChannel=maxOf(r,g,b); val chroma=maxChannel-minOf(r,g,b)
            val vibranceFactor=1f+vibrance*(1f-chroma/255f)
            val clarityFactor=1f+clarity*0.35f*midWeight
            r=gray+(r-gray)*saturation*vibranceFactor
            g=gray+(g-gray)*saturation*vibranceFactor
            b=gray+(b-gray)*saturation*vibranceFactor
            r=127.5f+(r-127.5f)*clarityFactor
            g=127.5f+(g-127.5f)*clarityFactor
            b=127.5f+(b-127.5f)*clarityFactor
            if(sharpenSource!=null && sharpen>0f) {
                val blurR=neighborAverage(sharpenSource,width,height,x,y,16)
                val blurG=neighborAverage(sharpenSource,width,height,x,y,8)
                val blurB=neighborAverage(sharpenSource,width,height,x,y,0)
                r+=((r-blurR)*sharpen*0.7f); g+=((g-blurG)*sharpen*0.7f); b+=((b-blurB)*sharpen*0.7f)
            }
            val ri=r.toInt().coerceIn(0,255);val gi=g.toInt().coerceIn(0,255);val bi=b.toInt().coerceIn(0,255)
            if(hueShift!=0f) { android.graphics.Color.RGBToHSV(ri,gi,bi,hsv);hsv[0]=(hsv[0]+hueShift+360f)%360f;val shifted=android.graphics.Color.HSVToColor(a,hsv);pixels[i]=shifted } else pixels[i]=(a shl 24) or (ri shl 16) or (gi shl 8) or bi
        }
        output.setPixels(pixels,0,width,0,0,width,height)
        return output
    }

    private fun neighborAverage(pixels:IntArray,width:Int,height:Int,x:Int,y:Int,shift:Int):Float {
        var sum=0; var count=0
        for(dy in -1..1) for(dx in -1..1) {
            val nx=(x+dx).coerceIn(0,width-1); val ny=(y+dy).coerceIn(0,height-1)
            sum += (pixels[ny*width+nx] shr shift) and 255; count++
        }
        return sum.toFloat()/count
    }
}
