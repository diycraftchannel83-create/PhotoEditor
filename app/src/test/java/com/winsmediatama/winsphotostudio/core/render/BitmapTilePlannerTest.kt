package com.winsmediatama.winsphotostudio.core.render

import android.os.CancellationSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BitmapTilePlannerTest {
    @Test fun plansNonOverlappingTilesAndShrinksEdges() {
        val tiles=BitmapTilePlanner.plan(1025,513,512).toList()
        assertEquals(6,tiles.size)
        assertEquals(512,tiles.first().width)
        assertEquals(1,tiles[2].width)
        assertEquals(1,tiles.last().height)
        assertEquals(1,tiles.last().width)
    }

    @Test fun tileCountUsesLongForLargeGrid() {
        assertEquals(196L,BitmapTilePlanner.count(10000,10000,720))
    }

    @Test fun cancellationStopsTileIteration() {
        val signal=CancellationSignal()
        val iterator=BitmapTilePlanner.plan(5000,5000,512,signal).iterator()
        assertEquals(true,iterator.hasNext())
        iterator.next()
        signal.cancel()
        assertThrows(android.os.OperationCanceledException::class.java){iterator.next()}
    }

    @Test fun rejectsInvalidSourceAndTileSize() {
        assertThrows(IllegalArgumentException::class.java){BitmapTilePlanner.plan(0,20)}
        assertThrows(IllegalArgumentException::class.java){BitmapTilePlanner.plan(20,20,3000)}
    }
}
