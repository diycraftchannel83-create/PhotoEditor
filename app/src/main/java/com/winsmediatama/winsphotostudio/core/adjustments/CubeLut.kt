package com.winsmediatama.winsphotostudio.core.adjustments

import android.graphics.Bitmap

/** Parser and CPU applier for common .cube 1D/3D LUT files. */
class CubeLut private constructor(
    val title:String?,
    val domainMin:FloatArray,
    val domainMax:FloatArray,
    val size:Int,
    private val values:FloatArray,
    val is3d:Boolean
) {
    fun apply(source:Bitmap):Bitmap {
        require(source.width.toLong()*source.height<=24_000_000L) { "LUT processing exceeds 24 MP safety limit" }
        val w=source.width;val h=source.height;val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h)
        for(i in pixels.indices){val p=pixels[i];val r=normalize((p shr 16 and 255)/255f,0);val g=normalize((p shr 8 and 255)/255f,1);val b=normalize((p and 255)/255f,2)
            val out=if(is3d)sample3d(r,g,b) else sample1d(r,g,b)
            val rr=(out[0].coerceIn(0f,1f)*255).toInt().coerceIn(0,255);val gg=(out[1].coerceIn(0f,1f)*255).toInt().coerceIn(0,255);val bb=(out[2].coerceIn(0f,1f)*255).toInt().coerceIn(0,255);pixels[i]=(p and 0xFF000000) or (rr shl 16) or (gg shl 8) or bb
        }
        return Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).apply{setPixels(pixels,0,w,0,0,w,h)}
    }
    private fun normalize(v:Float,channel:Int)=((v-domainMin[channel])/(domainMax[channel]-domainMin[channel]).coerceAtLeast(0.00001f)).coerceIn(0f,1f)
    private fun sample1d(r:Float,g:Float,b:Float):FloatArray {
        fun sample(v:Float,channel:Int):Float{val pos=v*(size-1);val low=pos.toInt().coerceIn(0,size-1);val high=(low+1).coerceIn(0,size-1);val t=pos-low.toFloat();return values[low*3+channel]*(1-t)+values[high*3+channel]*t}
        return floatArrayOf(sample(r,0),sample(g,1),sample(b,2))
    }
    private fun sample3d(r:Float,g:Float,b:Float):FloatArray {
        val x=r*(size-1);val y=g*(size-1);val z=b*(size-1)
        val x0=x.toInt().coerceIn(0,size-1);val y0=y.toInt().coerceIn(0,size-1);val z0=z.toInt().coerceIn(0,size-1)
        val x1=(x0+1).coerceAtMost(size-1);val y1=(y0+1).coerceAtMost(size-1);val z1=(z0+1).coerceAtMost(size-1)
        val tx=x-x0.toFloat();val ty=y-y0.toFloat();val tz=z-z0.toFloat()
        fun at(ix:Int,iy:Int,iz:Int,c:Int)=values[((iz*size+iy)*size+ix)*3+c]
        return FloatArray(3){c->
            val c00=at(x0,y0,z0,c)*(1-tx)+at(x1,y0,z0,c)*tx
            val c10=at(x0,y1,z0,c)*(1-tx)+at(x1,y1,z0,c)*tx
            val c01=at(x0,y0,z1,c)*(1-tx)+at(x1,y0,z1,c)*tx
            val c11=at(x0,y1,z1,c)*(1-tx)+at(x1,y1,z1,c)*tx
            val c0=c00*(1-ty)+c10*ty;val c1=c01*(1-ty)+c11*ty;c0*(1-tz)+c1*tz
        }
    }
    companion object {
        fun parse(text:String):CubeLut {
            require(text.length<=2_000_000) { "LUT text exceeds 2 MB" }
            var title:String?=null;var oneSize:Int?=null;var threeSize:Int?=null
            var min=floatArrayOf(0f,0f,0f);var max=floatArrayOf(1f,1f,1f)
            val table=mutableListOf<FloatArray>()
            text.lineSequence().forEach{raw->val line=raw.substringBefore('#').trim();if(line.isBlank())return@forEach
                val parts=line.split(Regex("\\s+"))
                when(parts[0].uppercase()){
                    "TITLE"->title=line.removePrefix("TITLE").trim().trim('"')
                    "LUT_1D_SIZE"->{oneSize=parts.getOrNull(1)?.toIntOrNull()}
                    "LUT_3D_SIZE"->{threeSize=parts.getOrNull(1)?.toIntOrNull()}
                    "DOMAIN_MIN"->{require(parts.size>=4);min=floatArrayOf(parts[1].toFloat(),parts[2].toFloat(),parts[3].toFloat())}
                    "DOMAIN_MAX"->{require(parts.size>=4);max=floatArrayOf(parts[1].toFloat(),parts[2].toFloat(),parts[3].toFloat())}
                    else->{if(parts.size>=3){val row=parts.take(3).map{it.toFloatOrNull()?:throw IllegalArgumentException("Invalid LUT value")};table+=row.toFloatArray()}}
                }
            }
            require(min.indices.all{max[it]>min[it]}){"Invalid LUT domain"}
            val size=threeSize?:oneSize?:throw IllegalArgumentException("Missing LUT size")
            val is3d=threeSize!=null
            require(size in 2..(if(is3d)64 else 4096)){"Unsupported LUT size"}
            val expected=if(is3d)size*size*size else size
            require(table.size==expected){"LUT table length mismatch: expected $expected, got ${table.size}"}
            return CubeLut(title,min,max,size,table.flatMap{it.asList()}.toFloatArray(),is3d)
        }
    }
}
