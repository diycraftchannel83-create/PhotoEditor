package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class BitmapRenderSchedulerTest {
    @Test
    fun cacheHitReturnsIndependentCallerOwnedBitmap() {
        val scheduler=BitmapRenderScheduler(maxCacheBytes=1024L)
        try {
            var renders=0
            val first=scheduler.request("sample-v1") {
                renders++
                Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
            }.get(3,TimeUnit.SECONDS)
            val second=scheduler.request("sample-v1") {
                renders++
                Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
            }.get(3,TimeUnit.SECONDS)
            assertEquals(1,renders)
            assertNotSame(first,second)
            first.eraseColor(Color.GREEN)
            assertEquals(Color.RED,second.getPixel(0,0))
            assertTrue(scheduler.cacheEntryCount()>=1)
            first.recycle()
            second.recycle()
        } finally { scheduler.close() }
    }

    @Test
    fun oversizedCacheEntryIsNotRetained() {
        val scheduler=BitmapRenderScheduler(maxCacheBytes=8L)
        try {
            val result=scheduler.request("large") {
                Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888)
            }.get(3,TimeUnit.SECONDS)
            assertEquals(0,scheduler.cacheEntryCount())
            result.recycle()
        } finally { scheduler.close() }
    }
}
