package com.ruishanio.taskpilot.tool.cache.impl

import com.ruishanio.taskpilot.tool.cache.iface.Cache
import com.ruishanio.taskpilot.tool.cache.iface.CacheLoader
import java.util.concurrent.ConcurrentHashMap

/**
 * 空缓存实现，所有读操作都视为未命中，仅在显式提供 loader 时尝试临时加载。
 */
class NoCache<K, V> : Cache<K, V> {
    override fun put(key: K, obj: V) = Unit

    override fun get(key: K): V? = null

    override fun getIfPresent(key: K): V? = null

    override fun get(key: K, cacheLoader: CacheLoader<K, V>?): V? =
        try {
            if (cacheLoader == null) {
                null
            } else {
                cacheLoader.load(key)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    override fun containsKey(key: K): Boolean = false

    override fun asMap(): Map<K, V> = ConcurrentHashMap()

    override fun size(): Int = 0

    override fun isFull(): Boolean = false

    override fun isEmpty(): Boolean = false

    override fun remove(key: K) = Unit

    override fun prune(): Int = 0

    override fun clear() = Unit

    override fun hitCount(): Long = 0

    override fun missCount(): Long = 0

    override fun capacity(): Int = 0

    override fun timeout(): Long = 0

    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
