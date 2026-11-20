package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.CancellationSignal
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * Streaming PNG encoder for a tile set. Avoids allocating a full flattened canvas.
 * The tile list must cover the canvas exactly with non-overlapping rectangles.
 * Each PNG scanline uses filter type 0; memory is bounded by one output row and a small deflate buffer.
 */
object TiledPngEncoder {
    private const val MAX_DIMENSION = 100_000
    private const val DEFLATE_BUFFER = 32 * 1024

    fun encodeCompositeTiles(
        tiles: List<TiledLayerCompositeRenderer.CompositeTile>,
        width: Int,
        height: Int,
        output: OutputStream,
        cancellation: CancellationSignal? = null
    ) = encode(
        tiles.map { BitmapTileProcessor.ProcessedTile(Rect(it.rect), it.bitmap) },
        width, height, output, cancellation
    )

    fun encodeHaloTiles(
        tiles: List<BitmapHaloTileProcessor.HaloTile>,
        width: Int,
        height: Int,
        output: OutputStream,
        cancellation: CancellationSignal? = null
    ) = encode(
        tiles.map { BitmapTileProcessor.ProcessedTile(Rect(it.coreRect), it.bitmap) },
        width, height, output, cancellation
    )

    fun encode(
        tiles: List<BitmapTileProcessor.ProcessedTile>,
        width: Int,
        height: Int,
        output: OutputStream,
        cancellation: CancellationSignal? = null
    ) {
        require(width in 1..MAX_DIMENSION && height in 1..MAX_DIMENSION) { "PNG dimensions exceed supported bounds" }
        validateTileCoverage(tiles.map { it.sourceRect to it.bitmap }, width, height)
        cancellation?.throwIfCanceled()
        output.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))
        writeChunk(output, "IHDR", byteArrayOf(
            (width ushr 24).toByte(), (width ushr 16).toByte(), (width ushr 8).toByte(), width.toByte(),
            (height ushr 24).toByte(), (height ushr 16).toByte(), (height ushr 8).toByte(), height.toByte(),
            8, 6, 0, 0, 0
        ))
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION)
        val compressed = ByteArray(DEFLATE_BUFFER)
        val row = ByteArray(width * 4 + 1)
        val starts = tiles.groupBy { it.sourceRect.top }
        val ends = tiles.groupBy { it.sourceRect.bottom }
        val active = mutableListOf<BitmapTileProcessor.ProcessedTile>()
        try {
            for (y in 0 until height) {
                cancellation?.throwIfCanceled()
                ends[y]?.let { removed -> active.removeAll(removed.toSet()) }
                starts[y]?.let { added -> active.addAll(added) }
                if (starts.containsKey(y) || ends.containsKey(y)) active.sortBy { it.sourceRect.left }
                row[0] = 0 // PNG filter: None
                var cursor = 1
                active.forEach { tile ->
                    val rect = tile.sourceRect
                    val localY = y - rect.top
                    val pixels = IntArray(rect.width())
                    tile.bitmap.getPixels(pixels, 0, rect.width(), 0, localY, rect.width(), 1)
                    pixels.forEach { pixel ->
                        row[cursor++] = (pixel ushr 16).toByte()
                        row[cursor++] = (pixel ushr 8).toByte()
                        row[cursor++] = pixel.toByte()
                        row[cursor++] = (pixel ushr 24).toByte()
                    }
                }
                require(cursor == row.size) { "Tile row coverage mismatch at y=$y" }
                deflater.setInput(row)
                while (!deflater.needsInput()) {
                    cancellation?.throwIfCanceled()
                    val count = deflater.deflate(compressed)
                    if (count > 0) writeChunk(output, "IDAT", compressed.copyOf(count))
                    else if (!deflater.needsInput()) throw IllegalStateException("PNG deflater made no progress")
                }
            }
            deflater.finish()
            while (!deflater.finished()) {
                cancellation?.throwIfCanceled()
                val count = deflater.deflate(compressed)
                if (count > 0) writeChunk(output, "IDAT", compressed.copyOf(count))
                else if (!deflater.finished()) throw IllegalStateException("PNG deflater made no progress during finish")
            }
            writeChunk(output, "IEND", byteArrayOf())
            output.flush()
        } finally {
            deflater.end()
        }
    }

    private fun validateTileCoverage(tiles: List<Pair<Rect, Bitmap>>, width: Int, height: Int) {
        require(tiles.isNotEmpty()) { "Tile set is empty" }
        tiles.forEach { (rect, bitmap) ->
            require(!bitmap.isRecycled && bitmap.width == rect.width() && bitmap.height == rect.height()) {
                "Tile dimensions or bitmap state invalid"
            }
            require(rect.left >= 0 && rect.top >= 0 && rect.right <= width && rect.bottom <= height && !rect.isEmpty) {
                "Tile rectangle lies outside PNG canvas"
            }
        }
        // Validate only at vertical boundaries. Coverage is constant between boundaries,
        // so large canvases avoid a tile-list scan for every pixel row.
        val starts = tiles.groupBy { it.first.top }
        val ends = tiles.groupBy { it.first.bottom }
        val boundaries = (starts.keys + ends.keys + setOf(0, height)).distinct().sorted()
        val active = mutableListOf<Pair<Rect, Bitmap>>()
        boundaries.zipWithNext().forEach { (top, bottom) ->
            ends[top]?.let { removed -> active.removeAll(removed.toSet()) }
            starts[top]?.let { added -> active.addAll(added) }
            if (bottom <= top) return@forEach
            var nextX = 0
            active.sortedBy { it.first.left }.forEach { (rect, _) ->
                require(rect.left == nextX) { "Tile row has gap or overlap at y=$top, x=$nextX" }
                nextX = rect.right
            }
            require(nextX == width) { "Tile row does not cover PNG width at y=$top" }
        }
    }

    private fun writeChunk(output: OutputStream, type: String, data: ByteArray) {
        writeInt(output, data.size)
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        output.write(typeBytes)
        output.write(data)
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        writeInt(output, crc.value.toInt())
    }

    private fun writeInt(output: OutputStream, value: Int) {
        output.write((value ushr 24) and 255)
        output.write((value ushr 16) and 255)
        output.write((value ushr 8) and 255)
        output.write(value and 255)
    }
}
