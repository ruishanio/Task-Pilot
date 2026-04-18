package com.ruishanio.taskpilot.tool.cache.impl

import com.ruishanio.taskpilot.tool.cache.iface.Cache
import com.ruishanio.taskpilot.tool.cache.iface.CacheListener
import com.ruishanio.taskpilot.tool.cache.iface.CacheLoader
import com.ruishanio.taskpilot.tool.cache.model.CacheKey
import com.ruishanio.taskpilot.tool.cache.model.CacheObject
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.LongAdder
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import org.slf4j.LoggerFactory

/**
 * 基于分段写锁的缓存基类。
 * 这里保留原来的锁粒度和计数方式，不在 Kotlin 迁移阶段改变并发语义。
 */
abstract class ReentrantCache<K, V> : Cache<K, V> {
    protected var timeout: Long = 0
    protected var capacity: Int = 0
    protected var expireType: Boolean = false
    protected val writeLockMap: ConcurrentMap<Long, Lock> = ConcurrentHashMap()
    protected val writeLockCount: Int = 100
    protected lateinit var cacheMap: MutableMap<CacheKey<K>, CacheObject<K, V>>
    protected val hitCounter = LongAdder()
    protected val missCounter = LongAdder()
    protected var listener: CacheListener<K, V>? = null
    protected var loader: CacheLoader<K, V>? = null

    private fun getKeyLock(key: K?): Lock {
        val keyHash = if (key != null) key.hashCode().toLong() % writeLockCount else -1L
        return writeLockMap.computeIfAbsent(keyHash) { ReentrantLock() }
    }

    /**
     * 统一通过显式泛型包装缓存键，避免 Kotlin 在空判断后把 `K` 收窄成 `K & Any`。
     */
    private fun wrapKey(key: K): CacheKey<K> = CacheKey(key)

    override fun put(key: K, obj: V) {
        if (key == null) {
            return
        }

        val writeLock = getKeyLock(key)
        writeLock.lock()
        try {
            val cacheKey: CacheKey<K> = wrapKey(key)
            val cacheObject: CacheObject<K, V> = CacheObject(key, obj, timeout, expireType)
            if (cacheMap.containsKey(cacheKey)) {
                cacheMap[cacheKey] = cacheObject
            } else {
                if (isFull()) {
                    doPrune()
                }
                cacheMap[cacheKey] = cacheObject
            }
        } finally {
            writeLock.unlock()
        }
    }

    override fun get(key: K): V? = get(key, loader)

    override fun getIfPresent(key: K): V? = get(key, null)

    override fun get(key: K, cacheLoader: CacheLoader<K, V>?): V? {
        var value = getOrRemoveExpired(key, true, true)
        if (value == null && cacheLoader != null) {
            val writeLock = getKeyLock(key)
            writeLock.lock()
            try {
                value = getOrRemoveExpired(key, true, false)
                if (value == null) {
                    val loadedValue = cacheLoader.load(key)
                    value = loadedValue
                    if (loadedValue != null) {
                        put(key, loadedValue)
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            } finally {
                writeLock.unlock()
            }
        }
        return value
    }

    /**
     * 缓存项过期时在读路径内直接删除，避免脏数据继续被命中。
     */
    private fun getOrRemoveExpired(key: K, isUpdateLastAccess: Boolean, isUpdateCount: Boolean): V? {
        var cacheObject: CacheObject<K, V>? = null
        val writeLock = getKeyLock(key)
        writeLock.lock()
        try {
            cacheObject = cacheMap[wrapKey(key)]
            if (cacheObject != null && cacheObject.isExpired()) {
                removeWithoutLock(key)
                onRemove(cacheObject)
                cacheObject = null
            }
        } finally {
            writeLock.unlock()
        }

        if (isUpdateCount) {
            if (cacheObject == null) {
                missCounter.increment()
            } else {
                hitCounter.increment()
            }
        }

        return cacheObject?.get(isUpdateLastAccess)
    }

    override fun containsKey(key: K): Boolean = getOrRemoveExpired(key, false, false) != null

    override fun asMap(): Map<K, V> {
        val result = LinkedHashMap<K, V>()
        for (cacheObject in cacheMap.values) {
            if (!cacheObject.isExpired()) {
                result[cacheObject.key] = cacheObject.value
            }
        }
        return result
    }

    override fun size(): Int = cacheMap.size

    override fun isFull(): Boolean = capacity > 0 && cacheMap.size >= capacity

    override fun isEmpty(): Boolean = cacheMap.isEmpty()

    override fun remove(key: K) {
        val cacheObject: CacheObject<K, V>?
        val writeLock = getKeyLock(key)
        writeLock.lock()
        try {
            cacheObject = removeWithoutLock(key)
        } finally {
            writeLock.unlock()
        }
        onRemove(cacheObject)
    }

    protected fun removeWithoutLock(key: K): CacheObject<K, V>? = cacheMap.remove(wrapKey(key))

    /**
     * 删除监听器异常只记录日志，不反向影响主缓存流程。
     */
    protected fun onRemove(cacheObject: CacheObject<K, V>?) {
        if (listener != null && cacheObject != null) {
            try {
                listener!!.onRemove(cacheObject.key, cacheObject.value)
            } catch (e: Exception) {
                logger.error("执行缓存移除监听器时发生异常，cacheObject:{}", cacheObject, e)
            }
        }
    }

    final override fun prune(): Int {
        val writeLock = getKeyLock(null)
        writeLock.lock()
        try {
            return doPrune()
        } finally {
            writeLock.unlock()
        }
    }

    protected abstract fun doPrune(): Int

    override fun clear() {
        val writeLock = getKeyLock(null)
        writeLock.lock()
        try {
            cacheMap.clear()
        } finally {
            writeLock.unlock()
        }
    }

    override fun hitCount(): Long = hitCounter.sum()

    override fun missCount(): Long = missCounter.sum()

    override fun capacity(): Int = capacity

    override fun timeout(): Long = timeout

    override fun setListener(listener: CacheListener<K, V>?): ReentrantCache<K, V> {
        this.listener = listener
        return this
    }

    override fun setLoader(listener: CacheLoader<K, V>?): ReentrantCache<K, V> {
        this.loader = listener
        return this
    }

    override fun toString(): String {
        val writeLock = getKeyLock(null)
        writeLock.lock()
        try {
            return cacheMap.toString()
        } finally {
            writeLock.unlock()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ReentrantCache::class.java)
        private const val serialVersionUID: Long = 42L
    }
}
