package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.CancellationSignal
import com.winsmediatama.winsphotostudio.core.editor.EditorBlendMode
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.editor.LayerMaskProcessor
import com.winsmediatama.winsphotostudio.core.importer.BitmapTilePlanner

/**
 * Bounded-memory tile compositor for simple bitmap-layer stacks.
 *
 * It deliberately rejects operation stacks, groups and clipping-to-below instead of silently
 * flattening incorrectly. Layer bitmaps are treated as canvas-aligned; each tile extracts the same
 * source rectangle from each layer, applies masks and opacity, then composites into a tile canvas.
 */
object TiledLayerCompositeRenderer {
    data class CompositeTile(val rect: Rect, val bitmap: Bitmap)

    /**
     * Composite and encode one scanline at a time. Only one horizontal tile (plus its masked
     * temporary) is allocated per layer at a time, so output memory does not scale with canvas area.
     * This is the preferred PNG path for supported flat bitmap stacks.
     */
    fun encodeStreamingPng(
        layers: List<EditorLayer>,
        width: Int,
        height: Int,
        output: java.io.OutputStream,
        tileWidth: Int = 512,
        cancellation: CancellationSignal? = null,
        cancelCheck: (() -> Unit)? = null
    ) {
        require(width > 0 && height > 0 && tileWidth > 0)
        require(layers.isNotEmpty())
        require(layers.none { it.groupId != null || it.clipToBelow }) {
            "Streaming compositor currently supports flat layers without clipping"
        }
        require(layers.none { it.operations.isNotEmpty() }) {
            "Streaming compositor currently supports layers without operation stacks"
        }
        require(layers.all { it.bitmap.width == width && it.bitmap.height == height }) {
            "Streaming compositor requires canvas-sized layer bitmaps"
        }
        require(layers.all { it.mask == null || (it.mask.width == width && it.mask.height == height) }) {
            "Streaming compositor requires canvas-sized layer masks"
        }
        TiledPngEncoder.encodeRows(width, height, output, cancellation, cancelCheck) { y, rowPixels ->
            var left = 0
            while (left < width) {
                cancellation?.throwIfCanceled()
                cancelCheck?.invoke()
                val segmentWidth = (width - left).coerceAtMost(tileWidth)
                val segmentRect = Rect(left, y, left + segmentWidth, y + 1)
                val composite = Bitmap.createBitmap(segmentWidth, 1, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(composite)
                    layers.filter { it.visible && !it.bitmap.isRecycled }.forEach { layer ->
                        cancellation?.throwIfCanceled()
                        cancelCheck?.invoke()
                        val cropped = extractAlignedRegion(layer.bitmap, segmentRect)
                        var masked: Bitmap? = null
                        try {
                            masked = layer.mask?.takeUnless { it.isRecycled }?.let { mask ->
                                val maskRow = extractAlignedRegion(mask, segmentRect)
                                try { LayerMaskProcessor.apply(cropped, maskRow) }
                                finally { if (!maskRow.isRecycled) maskRow.recycle() }
                            }
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                                alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
                                xfermode = blendMode(layer.blendMode)
                            }
                            if (layer.blendMode == EditorBlendMode.DIFFERENCE) drawDifferenceRow(composite, masked ?: cropped, layer.opacity)
                            else canvas.drawBitmap(masked ?: cropped, 0f, 0f, paint)
                            paint.xfermode = null
                        } finally {
                            masked?.let { if (!it.isRecycled) it.recycle() }
                            if (!cropped.isRecycled) cropped.recycle()
                        }
                    }
                    composite.getPixels(rowPixels, left, width, 0, 0, segmentWidth, 1)
                } finally {
                    if (!composite.isRecycled) composite.recycle()
                }
                left += segmentWidth
            }
        }
    }

    /**
     * Streaming multi-layer PNG compositor with per-layer non-destructive replay.
     * Keeps one horizontal tile band per visible layer at a time, then releases it.
     * Flat canvas-aligned layers, masks, opacity and blend modes are supported; groups,
     * clipping and coordinate-dependent effects are deliberately rejected.
     */
    fun encodeStreamingOperationLayers(
        layers: List<EditorLayer>,
        width: Int,
        height: Int,
        output: java.io.OutputStream,
        tileSize: Int = 256,
        cancellation: CancellationSignal? = null,
        cancelCheck: (() -> Unit)? = null
    ) {
        require(width > 0 && height > 0 && tileSize in 32..2048)
        require(layers.isNotEmpty())
        require(layers.none { it.groupId != null || it.clipToBelow }) {
            "Streaming operation compositor does not support groups or clipping"
        }
        require(layers.all { it.bitmap.width == width && it.bitmap.height == height }) {
            "Layer bitmaps must match canvas dimensions"
        }
        require(layers.all { it.mask == null || (it.mask.width == width && it.mask.height == height) }) {
            "Layer masks must match canvas dimensions"
        }
        require(layers.filter { it.visible }.all { TiledOperationReplayProcessor.supportsHalo(it.operations) }) {
            "One or more layers contain unsupported operation stacks"
        }
        val visibleLayerCount = layers.count { it.visible && !it.bitmap.isRecycled }.coerceAtLeast(1)
        val bandHeight = minOf(tileSize, (4_000_000L / (width.toLong() * visibleLayerCount)).coerceAtLeast(1L).toInt())
        var activeBandTop = -1
        val activeTiles = mutableMapOf<String, TiledOperationReplayProcessor.OutputTile>()
        fun releaseBand() {
            activeTiles.values.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
            activeTiles.clear()
        }
        try {
            TiledPngEncoder.encodeRows(width, height, output, cancellation, cancelCheck) { y, row ->
                val bandTop = (y / bandHeight) * bandHeight
                if (bandTop != activeBandTop) {
                    releaseBand()
                    activeBandTop = bandTop
                    val bandBottom = minOf(height, bandTop + bandHeight)
                    layers.filter { it.visible && !it.bitmap.isRecycled }.forEach { layer ->
                        cancellation?.throwIfCanceled(); cancelCheck?.invoke()
                        val replayed = TiledOperationReplayProcessor.renderHaloTile(
                            layer.bitmap, Rect(0, bandTop, width, bandBottom), layer.operations, cancellation
                        )
                        activeTiles[layer.id] = replayed
                    }
                }
                var x = 0
                var left = 0
                while (left < width) {
                    cancellation?.throwIfCanceled(); cancelCheck?.invoke()
                    val tileWidth = minOf(tileSize, width - left)
                    val composite = Bitmap.createBitmap(tileWidth, 1, Bitmap.Config.ARGB_8888)
                    try {
                        val canvas = Canvas(composite)
                        layers.filter { it.visible && !it.bitmap.isRecycled }.forEach { layer ->
                            val replayed = requireNotNull(activeTiles[layer.id]).bitmap
                            val tilePixels = Bitmap.createBitmap(replayed, left, y - bandTop, tileWidth, 1)
                            var masked: Bitmap? = null
                            try {
                                masked = layer.mask?.let { mask ->
                                    val maskRow = Bitmap.createBitmap(mask, left, y, tileWidth, 1)
                                    try { LayerMaskProcessor.apply(tilePixels, maskRow) } finally { maskRow.recycle() }
                                }
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                                    alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
                                    xfermode = blendMode(layer.blendMode)
                                }
                                if (layer.blendMode == EditorBlendMode.DIFFERENCE) drawDifferenceRow(composite, masked ?: tilePixels, layer.opacity)
                                else canvas.drawBitmap(masked ?: tilePixels, 0f, 0f, paint)
                                paint.xfermode = null
                            } finally {
                                masked?.let { if (!it.isRecycled) it.recycle() }
                                if (!tilePixels.isRecycled) tilePixels.recycle()
                            }
                        }
                        composite.getPixels(row, left, width, 0, 0, tileWidth, 1)
                    } finally { if (!composite.isRecycled) composite.recycle() }
                    left += tileWidth
                    x += tileWidth
                }
                require(x == width)
            }
        } finally { releaseBand() }
    }

    /**
     * Group-aware streaming compositor. Groups are contiguous in the layer list (the editor's
     * flattened tree representation). Group opacity/blend is applied once to the group result,
     * while child masks, opacity, blend modes and operation replay are applied internally.
     * Clipping and Difference remain unsupported to preserve explicit parity boundaries.
     */
    fun encodeStreamingGroupedOperationLayers(
        layers: List<EditorLayer>,
        width: Int,
        height: Int,
        output: java.io.OutputStream,
        tileSize: Int = 256,
        cancellation: CancellationSignal? = null,
        cancelCheck: (() -> Unit)? = null
    ) {
        require(width > 0 && height > 0 && tileSize in 32..2048 && layers.isNotEmpty())
        require(layers.none { it.clipToBelow }) { "Grouped streaming currently rejects clipping" }
        require(layers.none { it.blendMode == EditorBlendMode.DIFFERENCE || it.groupBlendMode == EditorBlendMode.DIFFERENCE }) {
            "Difference blend is not yet supported by grouped streaming"
        }
        require(layers.all { it.bitmap.width == width && it.bitmap.height == height }) {
            "Grouped streaming requires canvas-aligned layers"
        }
        require(layers.all { it.mask == null || (it.mask.width == width && it.mask.height == height) }) {
            "Grouped streaming requires canvas-aligned masks"
        }
        require(layers.filter { it.visible }.all { TiledOperationReplayProcessor.supportsHalo(it.operations) }) {
            "Unsupported operation stack in grouped streaming"
        }
        val visibleCount=layers.count { it.visible && !it.bitmap.isRecycled }.coerceAtLeast(1)
        val bandHeight=minOf(tileSize,(4_000_000L/(width.toLong()*visibleCount)).coerceAtLeast(1L).toInt())
        var currentBand=-1
        val replayBand=mutableMapOf<String,Bitmap>()
        fun clearBand(){ replayBand.values.forEach{if(!it.isRecycled)it.recycle()};replayBand.clear() }
        fun drawLayer(canvas:Canvas,layer:EditorLayer,y:Int,left:Int,segmentWidth:Int,bandTop:Int){
            if(!layer.visible||layer.bitmap.isRecycled)return
            val replayed=requireNotNull(replayBand[layer.id])
            val rowBitmap=Bitmap.createBitmap(replayed,left,y-bandTop,segmentWidth,1)
            var masked:Bitmap?=null
            try{
                masked=layer.mask?.let{mask->
                    val maskRow=Bitmap.createBitmap(mask,left,y,segmentWidth,1)
                    try{LayerMaskProcessor.apply(rowBitmap,maskRow)}finally{maskRow.recycle()}
                }
                val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply{
                    alpha=(layer.opacity.coerceIn(0f,1f)*255).toInt()
                    xfermode=blendMode(layer.blendMode)
                }
                canvas.drawBitmap(masked?:rowBitmap,0f,0f,paint);paint.xfermode=null
            }finally{
                masked?.let{if(!it.isRecycled)it.recycle()}
                if(!rowBitmap.isRecycled)rowBitmap.recycle()
            }
        }
        try{
            TiledPngEncoder.encodeRows(width,height,output,cancellation,cancelCheck){y,row->
                val bandTop=(y/bandHeight)*bandHeight
                if(bandTop!=currentBand){
                    clearBand();currentBand=bandTop
                    val bandBottom=minOf(height,bandTop+bandHeight)
                    layers.filter{it.visible&&!it.bitmap.isRecycled}.forEach{layer->
                        cancellation?.throwIfCanceled();cancelCheck?.invoke()
                        replayBand[layer.id]=TiledOperationReplayProcessor.renderHaloTile(
                            layer.bitmap,Rect(0,bandTop,width,bandBottom),layer.operations,cancellation
                        ).bitmap
                    }
                }
                var left=0
                while(left<width){
                    cancellation?.throwIfCanceled();cancelCheck?.invoke()
                    val sw=minOf(tileSize,width-left)
                    val composite=Bitmap.createBitmap(sw,1,Bitmap.Config.ARGB_8888)
                    try{
                        val outer=Canvas(composite)
                        var index=0
                        while(index<layers.size){
                            val layer=layers[index]
                            if(layer.groupId.isNullOrBlank()){
                                drawLayer(outer,layer,y,left,sw,bandTop);index++;continue
                            }
                            val groupId=layer.groupId
                            val children=mutableListOf<EditorLayer>()
                            while(index<layers.size&&layers[index].groupId==groupId){children+=layers[index];index++}
                            val groupBitmap=Bitmap.createBitmap(sw,1,Bitmap.Config.ARGB_8888)
                            try{
                                val groupCanvas=Canvas(groupBitmap)
                                children.forEach{child->drawLayer(groupCanvas,child,y,left,sw,bandTop)}
                                val groupOpacity=(children.firstOrNull()?.groupOpacity?:1f).coerceIn(0f,1f)
                                val groupMode=children.firstOrNull()?.groupBlendMode?:EditorBlendMode.NORMAL
                                val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply{
                                    alpha=(groupOpacity*255).toInt();xfermode=blendMode(groupMode)
                                }
                                outer.drawBitmap(groupBitmap,0f,0f,paint);paint.xfermode=null
                            }finally{if(!groupBitmap.isRecycled)groupBitmap.recycle()}
                        }
                        composite.getPixels(row,left,width,0,0,sw,1)
                    }finally{if(!composite.isRecycled)composite.recycle()}
                    left+=sw
                }
            }
        }finally{clearBand()}
    }

    /**
     * Flat-layer streaming path with clip-to-below alpha. The clip source is the previous
     * visible layer's prepared alpha after its own mask, clipping and opacity, matching the
     * editor compositor's source-alpha semantics. Groups and Difference remain excluded.
     */
    fun encodeStreamingClippedOperationLayers(
        layers: List<EditorLayer>,
        width: Int,
        height: Int,
        output: java.io.OutputStream,
        tileSize: Int = 256,
        cancellation: CancellationSignal? = null,
        cancelCheck: (() -> Unit)? = null
    ) {
        require(width>0&&height>0&&tileSize in 32..2048&&layers.isNotEmpty())
        require(layers.none{it.groupId!=null}){"Clipping stream does not support groups"}
        require(layers.none{it.blendMode==EditorBlendMode.DIFFERENCE}){"Difference blend unsupported in clipping stream"}
        require(layers.all{it.bitmap.width==width&&it.bitmap.height==height}){"Canvas-aligned layers required"}
        require(layers.all { it.mask == null || (it.mask.width == width && it.mask.height == height) }) { "Canvas-aligned masks required" }
        require(layers.filter{it.visible}.all{TiledOperationReplayProcessor.supportsHalo(it.operations)}){"Unsupported operation replay"}
        val visibleCount=layers.count{it.visible&&!it.bitmap.isRecycled}.coerceAtLeast(1)
        val bandHeight=minOf(tileSize,(4_000_000L/(width.toLong()*visibleCount)).coerceAtLeast(1L).toInt())
        var bandTopActive=-1
        val replayBands=mutableMapOf<String,Bitmap>()
        fun clearBand(){replayBands.values.forEach{if(!it.isRecycled)it.recycle()};replayBands.clear()}
        try{
            TiledPngEncoder.encodeRows(width,height,output,cancellation,cancelCheck){y,row->
                val bandTop=(y/bandHeight)*bandHeight
                if(bandTop!=bandTopActive){
                    clearBand();bandTopActive=bandTop
                    val bottom=minOf(height,bandTop+bandHeight)
                    layers.filter{it.visible&&!it.bitmap.isRecycled}.forEach{layer->
                        cancellation?.throwIfCanceled();cancelCheck?.invoke()
                        replayBands[layer.id]=TiledOperationReplayProcessor.renderHaloTile(
                            layer.bitmap,Rect(0,bandTop,width,bottom),layer.operations,cancellation
                        ).bitmap
                    }
                }
                var left=0
                while(left<width){
                    cancellation?.throwIfCanceled();cancelCheck?.invoke()
                    val sw=minOf(tileSize,width-left)
                    val composite=Bitmap.createBitmap(sw,1,Bitmap.Config.ARGB_8888)
                    var previousAlphaRow:Bitmap?=null
                    try{
                        val canvas=Canvas(composite)
                        layers.filter{it.visible&&!it.bitmap.isRecycled}.forEach{layer->
                            val replay=requireNotNull(replayBands[layer.id])
                            val sourceRow=Bitmap.createBitmap(replay,left,y-bandTop,sw,1)
                            var masked:Bitmap?=null
                            var clipped:Bitmap?=null
                            var storedAlpha:Bitmap?=null
                            try{
                                masked=layer.mask?.let{mask->
                                    val maskRow=Bitmap.createBitmap(mask,left,y,sw,1)
                                    try{LayerMaskProcessor.apply(sourceRow,maskRow)}finally{maskRow.recycle()}
                                }
                                val prepared=masked?:sourceRow
                                clipped=if(layer.clipToBelow&&previousAlphaRow!=null)ClipAlphaProcessor.apply(prepared,previousAlphaRow!!) else prepared
                                val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply{
                                    alpha=(layer.opacity.coerceIn(0f,1f)*255).toInt()
                                    xfermode=blendMode(layer.blendMode)
                                }
                                canvas.drawBitmap(clipped!!,0f,0f,paint);paint.xfermode=null
                                val pixels=IntArray(sw);clipped!!.getPixels(pixels,0,sw,0,0,sw,1)
                                for(i in pixels.indices)pixels[i]=( ((pixels[i] ushr 24)*layer.opacity.coerceIn(0f,1f)).toInt() shl 24) or (pixels[i] and 0x00FFFFFF)
                                storedAlpha=Bitmap.createBitmap(sw,1,Bitmap.Config.ARGB_8888).also{it.setPixels(pixels,0,sw,0,0,sw,1)}
                                previousAlphaRow?.let{if(!it.isRecycled)it.recycle()}
                                previousAlphaRow=storedAlpha
                            }finally{
                                storedAlpha?.takeIf{it!==previousAlphaRow}?.let{if(!it.isRecycled)it.recycle()}
                                clipped?.takeIf{it!==sourceRow&&it!==masked}?.let{if(!it.isRecycled)it.recycle()}
                                masked?.let{if(!it.isRecycled)it.recycle()}
                                if(!sourceRow.isRecycled)sourceRow.recycle()
                            }
                        }
                        composite.getPixels(row,left,width,0,0,sw,1)
                    }finally{
                        previousAlphaRow?.let{if(!it.isRecycled)it.recycle()}
                        if(!composite.isRecycled)composite.recycle()
                    }
                    left+=sw
                }
            }
        }finally{clearBand()}
    }

    fun renderTiles(
        layers: List<EditorLayer>,
        width: Int,
        height: Int,
        tileSize: Int = 512,
        cancellation: CancellationSignal? = null
    ): List<CompositeTile> {
        require(width > 0 && height > 0) { "Canvas dimensions must be positive" }
        require(layers.isNotEmpty()) { "Layer stack must not be empty" }
        require(layers.none { it.groupId != null || it.clipToBelow }) {
            "Tile compositor currently supports flat layers without clipping"
        }
        require(layers.none { it.operations.isNotEmpty() }) {
            "Tile compositor currently supports bitmap layers without replayable operations"
        }
        val planned = BitmapTilePlanner.plan(width, height, tileSize)
        val outputs = mutableListOf<CompositeTile>()
        try {
            planned.forEach { rect ->
                cancellation?.throwIfCanceled()
                val tile = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(tile)
                    layers.filter { it.visible && !it.bitmap.isRecycled }.forEach { layer ->
                        cancellation?.throwIfCanceled()
                        val sourceRect = Rect(rect.left, rect.top, rect.right, rect.bottom)
                        val cropped = extractAlignedRegion(layer.bitmap, sourceRect)
                        var masked: Bitmap? = null
                        try {
                            masked = layer.mask?.takeUnless { it.isRecycled }?.let { mask ->
                                extractAlignedRegion(mask, sourceRect).let { maskTile ->
                                    try { LayerMaskProcessor.apply(cropped, maskTile) }
                                    finally { if (!maskTile.isRecycled) maskTile.recycle() }
                                }
                            }
                            val drawSource = masked ?: cropped
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                                alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
                                xfermode = blendMode(layer.blendMode)
                            }
                            canvas.drawBitmap(drawSource, 0f, 0f, paint)
                            paint.xfermode = null
                        } finally {
                            masked?.let { if (!it.isRecycled) it.recycle() }
                            if (!cropped.isRecycled) cropped.recycle()
                        }
                    }
                    cancellation?.throwIfCanceled()
                    outputs += CompositeTile(Rect(rect), tile)
                } catch (error: Throwable) {
                    if (!tile.isRecycled) tile.recycle()
                    throw error
                }
            }
            cancellation?.throwIfCanceled()
            return outputs
        } catch (error: Throwable) {
            outputs.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
            throw error
        }
    }

    fun stitch(
        tiles: List<CompositeTile>,
        width: Int,
        height: Int,
        maxPixels: Long = 24_000_000L,
        cancellation: CancellationSignal? = null
    ): Bitmap {
        require(width > 0 && height > 0 && width.toLong() * height <= maxPixels) {
            "Flattened composite exceeds configured pixel safety limit"
        }
        cancellation?.throwIfCanceled()
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            tiles.forEach { tile ->
                cancellation?.throwIfCanceled()
                canvas.drawBitmap(tile.bitmap, tile.rect.left.toFloat(), tile.rect.top.toFloat(), null)
            }
            cancellation?.throwIfCanceled()
            return output
        } catch (error: Throwable) {
            if (!output.isRecycled) output.recycle()
            throw error
        }
    }

    fun recycleTiles(tiles: List<CompositeTile>) {
        tiles.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
    }

    private fun drawDifferenceRow(target:Bitmap,source:Bitmap,opacity:Float) {
        require(target.width==source.width&&target.height==source.height)
        val w=target.width;val h=target.height
        val base=IntArray(w*h);target.getPixels(base,0,w,0,0,w,h)
        val src=IntArray(w*h);source.getPixels(src,0,w,0,0,w,h)
        val op=(opacity.coerceIn(0f,1f)*255).toInt()
        for(i in base.indices){
            val d=base[i];val s=src[i]
            val da=d ushr 24
            val sa=(s ushr 24)*op/255
            val outA=sa+da*(255-sa)/255
            if(outA<=0){base[i]=0;continue}
            val dr=(d shr 16)and 255;val dg=(d shr 8)and 255;val db=d and 255
            val sr=(s shr 16)and 255;val sg=(s shr 8)and 255;val sb=s and 255
            val r=(dr*(255-sa)+kotlin.math.abs(dr-sr)*sa)/255
            val g=(dg*(255-sa)+kotlin.math.abs(dg-sg)*sa)/255
            val b=(db*(255-sa)+kotlin.math.abs(db-sb)*sa)/255
            base[i]=(outA shl 24)or(r shl 16)or(g shl 8)or b
        }
        target.setPixels(base,0,w,0,0,w,h)
    }

    private fun extractAlignedRegion(source: Bitmap, rect: Rect): Bitmap {
        val output = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            val sourceRect = Rect(
                rect.left.coerceIn(0, source.width),
                rect.top.coerceIn(0, source.height),
                rect.right.coerceIn(0, source.width),
                rect.bottom.coerceIn(0, source.height)
            )
            if (!sourceRect.isEmpty) {
                val destRect = Rect(
                    sourceRect.left - rect.left,
                    sourceRect.top - rect.top,
                    sourceRect.right - rect.left,
                    sourceRect.bottom - rect.top
                )
                canvas.drawBitmap(source, sourceRect, destRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            }
            return output
        } catch (error: Throwable) {
            if (!output.isRecycled) output.recycle()
            throw error
        }
    }

    private fun blendMode(mode: EditorBlendMode) = android.graphics.PorterDuffXfermode(
        when (mode) {
            EditorBlendMode.NORMAL -> android.graphics.PorterDuff.Mode.SRC_OVER
            EditorBlendMode.MULTIPLY -> android.graphics.PorterDuff.Mode.MULTIPLY
            EditorBlendMode.SCREEN -> android.graphics.PorterDuff.Mode.SCREEN
            EditorBlendMode.OVERLAY -> android.graphics.PorterDuff.Mode.OVERLAY
            EditorBlendMode.DARKEN -> android.graphics.PorterDuff.Mode.DARKEN
            EditorBlendMode.LIGHTEN -> android.graphics.PorterDuff.Mode.LIGHTEN
            EditorBlendMode.ADD -> android.graphics.PorterDuff.Mode.ADD
            EditorBlendMode.DIFFERENCE -> android.graphics.PorterDuff.Mode.XOR
        }
    )
}
