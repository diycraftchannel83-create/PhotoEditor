package com.winsmediatama.winsphotostudio.core.render

/**
 * Integer dirty rectangle used to describe pixel regions affected by edits.
 * Rectangles are half-open: right/bottom are exclusive.
 */
data class RenderDirtyRect(val left:Int,val top:Int,val right:Int,val bottom:Int) {
    init { require(right >= left && bottom >= top) }
    val width:Int get()=right-left
    val height:Int get()=bottom-top
    val isEmpty:Boolean get()=width==0 || height==0
    fun intersect(bounds:RenderDirtyRect):RenderDirtyRect = RenderDirtyRect(
        maxOf(left,bounds.left),maxOf(top,bounds.top),minOf(right,bounds.right),minOf(bottom,bounds.bottom)
    ).let { if(it.right < it.left || it.bottom < it.top) RenderDirtyRect(it.left,it.top,it.left,it.top) else it }
    fun expand(radius:Int,bounds:RenderDirtyRect):RenderDirtyRect {
        require(radius>=0)
        return RenderDirtyRect(left-radius,top-radius,right+radius,bottom+radius).intersect(bounds)
    }
    fun union(other:RenderDirtyRect):RenderDirtyRect = RenderDirtyRect(
        minOf(left,other.left),minOf(top,other.top),maxOf(right,other.right),maxOf(bottom,other.bottom)
    )
    fun touches(other:RenderDirtyRect,margin:Int=0):Boolean {
        require(margin>=0)
        return left<=other.right+margin && right+margin>=other.left &&
            top<=other.bottom+margin && bottom+margin>=other.top
    }
    fun area():Long=width.toLong()*height.toLong()
    companion object { fun canvas(width:Int,height:Int)=RenderDirtyRect(0,0,width,height) }
}

/**
 * Coalesces dirty rectangles for future incremental compositor work.
 * To avoid pathological fragmentation, it collapses to the canvas rect when
 * dirty area or rectangle count crosses the configured thresholds.
 */
class RenderDirtyRegionAccumulator(
    private val canvas:RenderDirtyRect,
    private val maxRects:Int=32,
    private val fullCanvasAreaRatio:Float=0.55f,
    private val mergeMargin:Int=1
) {
    init {
        require(!canvas.isEmpty)
        require(maxRects>0)
        require(fullCanvasAreaRatio in 0.01f..1f)
        require(mergeMargin>=0)
    }
    private val regions= mutableListOf<RenderDirtyRect>()
    private var full=false
    fun add(rect:RenderDirtyRect,halo:Int=0) {
        require(halo>=0)
        if(full || rect.isEmpty) return
        var merged=rect.expand(halo,canvas).intersect(canvas)
        if(merged.isEmpty) return
        var changed=true
        while(changed) {
            changed=false
            val iterator=regions.iterator()
            while(iterator.hasNext()) {
                val existing=iterator.next()
                if(merged.touches(existing,mergeMargin)) {
                    merged=merged.union(existing).intersect(canvas)
                    iterator.remove()
                    changed=true
                }
            }
        }
        regions.add(merged)
        if(regions.size>maxRects || dirtyArea()>=canvas.area()*fullCanvasAreaRatio) {
            collapseToFull()
        }
    }
    fun snapshot():List<RenderDirtyRect> = if(full) listOf(canvas) else regions.toList()
    fun isFullCanvas():Boolean=full
    fun clear(){regions.clear();full=false}
    private fun dirtyArea():Long {
        // Regions are coalesced to avoid most overlap. This conservative sum can
        // overestimate coverage and intentionally favors a full redraw.
        return regions.sumOf { it.area() }
    }
    private fun collapseToFull(){regions.clear();regions.add(canvas);full=true}
}
