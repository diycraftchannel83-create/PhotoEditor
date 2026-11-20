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
        var idatBuffer = ByteArray(DEFLATE_BUFFER)
        var idatSize = 0
        val compressed = ByteArray(DEFLATE_BUFFER)
        val crcData = ByteArray(DEFLATE_BUFFER)
        fun flushIdat() {
            if (idatSize > 0) {
                writeChunk(output, "IDAT", idatBuffer.copyOf(idatSize))
                idatSize = 0
            }
        }
        val row = ByteArray(width * 4 + 1)
        try {
            for (y in 0 until height) {
                cancellation?.throwIfCanceled()
                row[0] = 0 // PNG filter: None
                var cursor = 1
                val rowTiles = tiles.filter { y >= it.sourceRect.top && y < it.sourceRect.bottom }.sortedBy { it.sourceRect.left }
                rowTiles.forEach { tile ->
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
                    val count = deflater.deflate(compressed)
                    if (count <= 0) break
                    if (idatSize + count > idatBuffer.size) flushIdat()
                    if (idatSize + count > idatBuffer.size) {
                        writeChunk(output, "IDAT", compressed.copyOf(count))
                    } else {
                        compressed.copyInto(idatBuffer, idatSize, 0, count)
                        idatSize += count
                    }
                }
            }
            deflater.finish()
            while (!deflater.finished()) {
                cancellation?.throwIfCanceled()
                val count = deflater.deflate(compressed)
                if (count <= 0) break
                if (idatSize + count > idatBuffer.size) flushIdat()
                if (idatSize + count > idatBuffer.size) writeChunk(output, "IDAT", compressed.copyOf(count))
                else {
                    compressed.copyInto(idatBuffer, idatSize, 0, count)
                    idatSize += count
                }
            }
            flushIdat()
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
        // Coverage is validated per scanline; overlapping regions would overfill row buffers.
        for (y in 0 until height) {
            var nextX = 0
            tiles.filter { y >= it.first.top && y < it.first.bottom }.sortedBy { it.first.left }.forEach { (rect, _) ->
                require(rect.left == nextX) { "Tile row has gap or overlap at y=$y, x=$nextX" }
                nextX = rect.right
            }
            require(nextX == width) { "Tile row does not cover PNG width at y=$y" }
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
