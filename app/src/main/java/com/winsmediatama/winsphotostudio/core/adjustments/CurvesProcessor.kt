package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap

/** Linear interpolation through ordered normalized control points. */
object CurvesProcessor {
    fun apply(source: Bitmap, curves: CurvesValues): Bitmap {
        val compositeLut=buildLut(curves.composite)
        val redLut=buildLut(curves.red.ifEmpty { listOf(CurvePoint(0f,0f),CurvePoint(1f,1f)) })
        val greenLut=buildLut(curves.green.ifEmpty { listOf(CurvePoint(0f,0f),CurvePoint(1f,1f)) })
        val blueLut=buildLut(curves.blue.ifEmpty { listOf(CurvePoint(0f,0f),CurvePoint(1f,1f)) })
        val result=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
        val pixels=IntArray(source.width*source.height)
        source.getPixels(pixels,0,source.width,0,0,source.width,source.height)
        for(i in pixels.indices) {
            val p=pixels[i]; val a=p ushr 24
            val r=compositeLut[(p shr 16) and 255]; val g=compositeLut[(p shr 8) and 255]; val b=compositeLut[p and 255]
            pixels[i]=(a shl 24) or (redLut[r] shl 16) or (greenLut[g] shl 8) or blueLut[b]
        }
        result.setPixels(pixels,0,source.width,0,0,source.width,source.height)
        return result
    }
    private fun buildLut(points:List<CurvePoint>):IntArray {
        val sorted=points.map { CurvePoint(it.x.coerceIn(0f,1f),it.y.coerceIn(0f,1f)) }.sortedBy { it.x }
        val safe=if(sorted.size<2) listOf(CurvePoint(0f,0f),CurvePoint(1f,1f)) else sorted
        return IntArray(256) { index ->
            val x=index/255f
            val left=safe.lastOrNull { it.x<=x } ?: safe.first()
            val right=safe.firstOrNull { it.x>=x } ?: safe.last()
            val span=right.x-left.x
            val y=if(span<=0.00001f) left.y else left.y+(right.y-left.y)*((x-left.x)/span)
            (y*255f).toInt().coerceIn(0,255)
        }
    }
}
