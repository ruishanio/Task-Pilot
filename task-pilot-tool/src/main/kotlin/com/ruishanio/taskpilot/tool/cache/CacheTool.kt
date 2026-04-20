package com.ruishanio.taskpilot.tool.cache

import com.ruishanio.taskpilot.tool.cache.iface.Cache
import com.ruishanio.taskpilot.tool.cache.iface.CacheListener
import com.ruishanio.taskpilot.tool.cache.iface.CacheLoader
import com.ruishanio.taskpilot.tool.cache.impl.FIFOCache
import com.ruishanio.taskpilot.tool.cache.impl.LFUCache
import com.ruishanio.taskpilot.tool.cache.impl.LRUCache
import com.ruishanio.taskpilot.tool.cache.impl.NoCache
import com.ruishanio.taskpilot.tool.cache.impl.UnlimitedCache
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

/**
 * 缓存构建器。
 * 保留原有 fluent API 与默认参数，避免测试代码和外部调用链在迁移后改写。
 */
class CacheTool<K, V> {
    protected var timeout: Long = 0
    protected var expireType: Boolean = false
    protected var capacity: Int = 0
    protected var cacheType: CacheType? = null
    protected var loader: CacheLoader<K, V>? = null
    protected var listener: CacheListener<K, V>? = null
    protected var pruneInterval: Long = 0
    protected var pruneTaskFuture: ScheduledFuture<*>? = null
    protected var cache: Cache<K, V>? = null

    fun expireAfterWrite(timeout: Long): CacheTool<K, V> {
        this.timeout = timeout
        this.expireType = true
        return this
    }

    fun expireAfterAccess(timeout: Long): CacheTool<K, V> {
        this.timeout = timeout
        this.expireType = false
        return this
    }

    fun capacity(capacity: Int): CacheTool<K, V> {
        this.capacity = capacity
        return this
    }

    fun cache(cacheType: CacheType?): CacheTool<K, V> {
        this.cacheType = cacheType
        return this
    }

    fun loader(cacheLoader: CacheLoader<K, V>?): CacheTool<K, V> {
        this.loader = cacheLoader
        return this
    }

    fun listener(listener: CacheListener<K, V>?): CacheTool<K, V> {
        this.listener = listener
        return this
    }

    fun pruneInterval(pruneInterval: Long): CacheTool<K, V> {
        this.pruneInterval = pruneInterval
        return this
    }

    /**
     * 构建缓存实例，并按需挂上监听器、加载器和定时清理任务。
     */
    fun build(): Cache<K, V> {
        val cacheInstance: Cache<K, V> = when (cacheType) {
            CacheType.NONE -> NoCache()
            CacheType.FIFO -> FIFOCache(capacity, timeout, expireType)
            CacheType.LFU -> LFUCache(capacity, timeout, expireType)
            CacheType.LRU -> LRUCache(capacity, timeout, expireType)
            CacheType.UNLIMITED -> {
                if (pruneInterval <= 0) {
                    pruneInterval = 5 * 1000L
                }
                UnlimitedCache(timeout, expireType)
            }
            else -> throw RuntimeException("cacheType invalid.")
        }

        if (listener != null) {
            cacheInstance.setListener(listener)
        }
        if (loader != null) {
            cacheInstance.setLoader(loader)
        }

        if (pruneInterval > 0) {
            pruneTaskFuture = schedule(
                Runnable {
                    try {
                        cacheInstance.prune()
                    } catch (e: Exception) {
                        logger.error("定时清理缓存时发生异常。", e)
                    }
                },
                pruneInterval
            )
        }

        cache = cacheInstance
        return cacheInstance
    }

    /**
     * 主动停止定时清理任务，不回收缓存内容本身。
     */
    fun stop() {
        pruneTaskFuture?.cancel(true)
    }

    private fun schedule(task: Runnable, delay: Long): ScheduledFuture<*> =
        pruneTimer.scheduleAtFixedRate(task, delay, delay, TimeUnit.MILLISECONDS)

    companion object {
        private val logger = LoggerFactory.getLogger(CacheTool::class.java)
        private val pruneTimer: ScheduledExecutorService = ScheduledThreadPoolExecutor(1).also { executor ->
            Runtime.getRuntime().addShutdownHook(Thread { executor.shutdownNow() })
        }
        fun <K, V> newCache(): CacheTool<K, V> = newLRUCache()
        fun <K, V> newFIFOCache(): CacheTool<K, V> = newFIFOCache(1000)
        fun <K, V> newFIFOCache(capacity: Int): CacheTool<K, V> =
            CacheTool<K, V>().cache(CacheType.FIFO).capacity(capacity)
        fun <K, V> newLRUCache(): CacheTool<K, V> = newLRUCache(1000)
        fun <K, V> newLRUCache(capacity: Int): CacheTool<K, V> =
            CacheTool<K, V>().cache(CacheType.LRU).capacity(capacity)
        fun <K, V> newLFUCache(): CacheTool<K, V> = newLFUCache(1000)
        fun <K, V> newLFUCache(capacity: Int): CacheTool<K, V> =
            CacheTool<K, V>().cache(CacheType.LFU).capacity(capacity)
        fun <K, V> newUnlimitedCache(): CacheTool<K, V> = newUnlimitedCache(0)
        fun <K, V> newUnlimitedCache(timeout: Int): CacheTool<K, V> =
            CacheTool<K, V>().cache(CacheType.UNLIMITED).expireAfterWrite(timeout.toLong())
    }
}
