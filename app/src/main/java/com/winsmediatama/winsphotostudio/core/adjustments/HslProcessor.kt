package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

enum class HslColorRange(val label:String,val center:Float) {
    RED("Red",0f),ORANGE("Orange",30f),YELLOW("Yellow",60f),GREEN("Green",120f),
    CYAN("Cyan",180f),BLUE("Blue",240f),PURPLE("Purple",270f),MAGENTA("Magenta",300f)
}
data class HslRangeAdjustment(val hueShift:Float=0f,val saturation:Float=0f,val lightness:Float=0f)
data class HslAdjustments(val ranges:Map<HslColorRange,HslRangeAdjustment> = emptyMap())

object HslProcessor {
    fun apply(source:Bitmap,adjustments:HslAdjustments):Bitmap {
        val output=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
        val pixels=IntArray(source.width*source.height);source.getPixels(pixels,0,source.width,0,0,source.width,source.height)
        val hsv=FloatArray(3)
        for(i in pixels.indices) {
            val p=pixels[i];val a=p ushr 24;val r=(p shr 16)and 255;val g=(p shr 8)and 255;val b=p and 255
            Color.RGBToHSV(r,g,b,hsv)
            val hue=hsv[0]
            var shift=0f;var sat=0f;var light=0f
            adjustments.ranges.forEach { (range,value)->
                val diff=abs(hue-range.center).let{minOf(it,360f-it)}
                val weight=(1f-diff/40f).coerceIn(0f,1f)
                shift+=value.hueShift*weight
                sat+=value.saturation*weight
                light+=value.lightness*weight
            }
            hsv[0]=(hue+shift+360f)%360f
            hsv[1]=(hsv[1]*(1f+sat)).coerceIn(0f,1f)
            hsv[2]=(hsv[2]+light).coerceIn(0f,1f)
            pixels[i]=Color.HSVToColor(a,hsv)
        }
        output.setPixels(pixels,0,source.width,0,0,source.width,source.height)
        return output
    }
}
