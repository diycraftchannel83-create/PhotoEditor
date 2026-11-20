package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Rect
import android.os.CancellationSignal

/** Safe source-space tile geometry for region-based image rendering. */
object BitmapTilePlanner {
    const val DEFAULT_TILE_SIZE=512
    const val MAX_TILE_SIZE=2048

    data class Tile(val column:Int,val row:Int,val sourceRect:Rect) {
        val width:Int get()=sourceRect.width()
        val height:Int get()=sourceRect.height()
    }

    fun count(width:Int,height:Int,tileSize:Int=DEFAULT_TILE_SIZE):Long {
        validate(width,height,tileSize)
        val columns=(width+tileSize-1)/tileSize
        val rows=(height+tileSize-1)/tileSize
        return columns.toLong()*rows
    }

    fun plan(width:Int,height:Int,tileSize:Int=DEFAULT_TILE_SIZE,cancellation:CancellationSignal?=null):Sequence<Tile> {
        validate(width,height,tileSize)
        val columns=(width+tileSize-1)/tileSize
        val rows=(height+tileSize-1)/tileSize
        return sequence {
            for(row in 0 until rows) {
                if(cancellation?.isCanceled==true) throw android.os.OperationCanceledException()
                for(column in 0 until columns) {
                    if(cancellation?.isCanceled==true) throw android.os.OperationCanceledException()
                    val left=column*tileSize
                    val top=row*tileSize
                    yield(Tile(column,row,Rect(left,top,minOf(width,left+tileSize),minOf(height,top+tileSize))))
                }
            }
        }
    }

    fun validate(width:Int,height:Int,tileSize:Int) {
        require(width>0&&height>0){"Tile source dimensions must be positive"}
        require(tileSize in 1..MAX_TILE_SIZE){"Tile size must be between 1 and $MAX_TILE_SIZE"}
        require(width<=100_000&&height<=100_000){"Tile source dimensions exceed supported limits"}
    }
}
