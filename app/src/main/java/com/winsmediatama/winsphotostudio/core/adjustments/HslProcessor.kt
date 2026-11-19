package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
        for(i in pixels.indices) {
            val p=pixels[i];val a=p ushr 24;val r=((p shr 16)and 255)/255f;val g=((p shr 8)and 255)/255f;val b=(p and 255)/255f
            val hsl=rgbToHsl(r,g,b);val hue=hsl[0]
            var shift=0f;var sat=0f;var light=0f
            adjustments.ranges.forEach { (range,value)->
                val diff=abs(hue-range.center).let{min(it,360f-it)}
                val weight=(1f-diff/40f).coerceIn(0f,1f)
                shift+=value.hueShift*weight;sat+=value.saturation*weight;light+=value.lightness*weight
            }
            hsl[0]=(hue+shift+360f)%360f
            hsl[1]=(hsl[1]*(1f+sat)).coerceIn(0f,1f)
            hsl[2]=(hsl[2]+light).coerceIn(0f,1f)
            val rgb=hslToRgb(hsl)
            pixels[i]=(a shl 24) or ((rgb[0]*255f).toInt().coerceIn(0,255) shl 16) or ((rgb[1]*255f).toInt().coerceIn(0,255) shl 8) or (rgb[2]*255f).toInt().coerceIn(0,255)
        }
        output.setPixels(pixels,0,source.width,0,0,source.width,source.height)
        return output
    }
    private fun rgbToHsl(r:Float,g:Float,b:Float):FloatArray {
        val max=max(r,max(g,b));val min=min(r,min(g,b));val delta=max-min
        val l=(max+min)/2f
        if(delta==0f)return floatArrayOf(0f,0f,l)
        val s=delta/(1f-abs(2f*l-1f))
        var h=when(max) {
            r->60f*(((g-b)/delta)%6f)
            g->60f*((b-r)/delta+2f)
            else->60f*((r-g)/delta+4f)
        }
        if(h<0f)h+=360f
        return floatArrayOf(h,s.coerceIn(0f,1f),l)
    }
    private fun hslToRgb(hsl:FloatArray):FloatArray {
        val h=hsl[0]/60f;val s=hsl[1];val l=hsl[2]
        val c=(1f-abs(2f*l-1f))*s
        val x=c*(1f-abs(h%2f-1f))
        val m=l-c/2f
        val (r,g,b)=when(h.toInt()) {
            0->Triple(c,x,0f);1->Triple(x,c,0f);2->Triple(0f,c,x);3->Triple(0f,x,c);4->Triple(x,0f,c);else->Triple(c,0f,x)
        }
        return floatArrayOf((r+m).coerceIn(0f,1f),(g+m).coerceIn(0f,1f),(b+m).coerceIn(0f,1f)
    }
}
