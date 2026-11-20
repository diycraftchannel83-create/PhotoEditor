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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

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
    fun cancellationSignalIsPropagatedToRenderer() {
        val scheduler=BitmapRenderScheduler(maxCacheBytes=1024L)
        val entered=CountDownLatch(1)
        val cancelled=AtomicBoolean(false)
        try {
            val future=scheduler.request("cancel-test") { signal ->
                entered.countDown()
                while(!signal.isCanceled) Thread.sleep(5)
                cancelled.set(signal.isCanceled)
                Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
            }
            assertTrue(entered.await(2,TimeUnit.SECONDS))
            future.cancel(true)
            scheduler.cancelAll()
            assertTrue(cancelled.get() || future.isCancelled)
        } finally { scheduler.close() }
    }

    @Test
    fun clearingCacheReleasesEntriesAndAllowsRerender() {
        val scheduler=BitmapRenderScheduler(maxCacheBytes=1024L)
        try {
            var renders=0
            val one=scheduler.request("invalidate") {
                renders++
                Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
            }.get(3,TimeUnit.SECONDS)
            one.recycle()
            assertEquals(1,scheduler.cacheEntryCount())
            scheduler.clearCache()
            assertEquals(0,scheduler.cacheEntryCount())
            val two=scheduler.request("invalidate") {
                renders++
                Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLUE)}
            }.get(3,TimeUnit.SECONDS)
            assertEquals(2,renders)
            assertEquals(Color.BLUE,two.getPixel(0,0))
            two.recycle()
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
