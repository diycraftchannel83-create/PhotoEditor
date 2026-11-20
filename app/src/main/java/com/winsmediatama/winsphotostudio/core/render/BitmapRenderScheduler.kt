package com.winsmediatama.winsphotostudio.core.render

import android.graphics.Bitmap
import android.os.CancellationSignal
import com.winsmediatama.winsphotostudio.core.editor.EditorLayer
import com.winsmediatama.winsphotostudio.core.editor.LayerCompositeRenderer
import java.io.Closeable
import java.util.LinkedHashMap
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Serialized preview-render queue with cancellation and a bounded bitmap LRU.
 *
 * Cache entries are owned by the scheduler. Render callbacks must return a fresh bitmap;
 * returned Future results are caller-owned copies and must be recycled by the caller.
 * Keys must include every source/edit parameter that can change output pixels.
 */
class BitmapRenderScheduler(
    private val maxCacheBytes: Long = 64L * 1024L * 1024L,
    threadName: String = "WinsPhoto-Render"
) : Closeable {
    init { require(maxCacheBytes >= 0L) }

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, threadName).apply { isDaemon = true }
    }
    private val cache = LinkedHashMap<String, Bitmap>(16, 0.75f, true)
    private val activeTasks = LinkedHashMap<Long, Pair<Future<*>, CancellationSignal>>()
    private val taskIds = AtomicLong(0L)
    private var cacheBytes = 0L
    private var closed = false

    /** A hit returns a caller-owned copy. A miss executes render on the queue. */
    fun request(key: String, render: (CancellationSignal) -> Bitmap): Future<Bitmap> {
        require(key.isNotBlank()) { "Render cache key must not be blank" }
        synchronized(this) {
            check(!closed) { "Render scheduler is closed" }
            cache[key]?.takeUnless { it.isRecycled }?.let { cached ->
                return FutureTask<Bitmap> { cached.copy(Bitmap.Config.ARGB_8888, true) }.also { it.run() }
            }
        }

        val id = taskIds.incrementAndGet()
        val cancellation = CancellationSignal()
        val task = FutureTask(Callable {
            synchronized(this) {
                check(!closed) { "Render scheduler is closed" }
                cache[key]?.takeUnless { it.isRecycled }?.let { cached ->
                    return@Callable cached.copy(Bitmap.Config.ARGB_8888, true)
                }
            }
            cancellation.throwIfCanceled()
            val generated = render(cancellation)
            try {
                cancellation.throwIfCanceled()
                require(!generated.isRecycled && generated.width > 0 && generated.height > 0) {
                    "Renderer returned an invalid bitmap"
                }
                synchronized(this) {
                    if (!closed && !cancellation.isCanceled) putCache(key, generated)
                }
                generated.copy(Bitmap.Config.ARGB_8888, true)
            } finally {
                if (!generated.isRecycled) generated.recycle()
            }
        })
        synchronized(this) {
            check(!closed) { "Render scheduler is closed" }
            activeTasks[id] = task to cancellation
        }
        executor.execute {
            try { task.run() }
            finally { synchronized(this) { activeTasks.remove(id) } }
        }
        return object : Future<Bitmap> by task {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                cancellation.cancel()
                return task.cancel(mayInterruptIfRunning)
            }
        }
    }

    /** Cache-keyed composite request for the editor's flat layer stack. */
    fun requestLayerStackComposite(
        layers: List<EditorLayer>,
        width: Int,
        height: Int
    ): Future<Bitmap> {
        val key = LayerRenderCacheKey.create(layers, width, height)
        return request(key) { cancellation -> LayerCompositeRenderer.render(layers, width, height, cancellation) }
    }

    /** Cancels queued and running work. Render callbacks should check signal periodically. */
    @Synchronized
    fun cancelAll() {
        activeTasks.values.forEach { (task, signal) -> signal.cancel(); task.cancel(true) }
        activeTasks.clear()
    }

    @Synchronized
    fun cachedBytes(): Long = cacheBytes

    @Synchronized
    fun cacheEntryCount(): Int = cache.size

    @Synchronized
    fun clearCache() {
        cache.values.forEach { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
        cache.clear()
        cacheBytes = 0L
    }

    private fun putCache(key: String, bitmap: Bitmap) {
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return
        val bytes = copy.allocationByteCount.toLong()
        if (bytes > maxCacheBytes || maxCacheBytes == 0L) {
            copy.recycle()
            return
        }
        cache.remove(key)?.let { old ->
            cacheBytes -= old.allocationByteCount.toLong()
            if (!old.isRecycled) old.recycle()
        }
        cache[key] = copy
        cacheBytes += bytes
        val iterator = cache.entries.iterator()
        while (cacheBytes > maxCacheBytes && iterator.hasNext()) {
            val entry = iterator.next()
            cacheBytes -= entry.value.allocationByteCount.toLong()
            if (!entry.value.isRecycled) entry.value.recycle()
            iterator.remove()
        }
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        activeTasks.values.forEach { (task, signal) -> signal.cancel(); task.cancel(true) }
        activeTasks.clear()
        executor.shutdownNow()
        clearCache()
    }
}
