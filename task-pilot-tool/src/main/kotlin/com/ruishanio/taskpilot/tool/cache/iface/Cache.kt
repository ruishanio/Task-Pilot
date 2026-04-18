package com.ruishanio.taskpilot.tool.cache.iface

import java.io.Serializable

/**
 * 缓存抽象接口。
 * 继续保留默认的 `setListener/setLoader` 空实现，兼容未显式覆写的 Java 缓存实现类。
 */
interface Cache<K, V> : Serializable {
    fun put(key: K, obj: V)

    /**
     * 读取缓存时继续允许未命中返回 `null`，避免 Kotlin 侧把 miss 误判成不可能分支。
     */
    fun get(key: K): V?

    fun getIfPresent(key: K): V?

    fun get(key: K, cacheLoader: CacheLoader<K, V>?): V?

    fun containsKey(key: K): Boolean

    fun asMap(): Map<K, V>

    fun size(): Int

    fun isFull(): Boolean

    fun isEmpty(): Boolean

    fun remove(key: K)

    fun prune(): Int

    fun clear()

    fun hitCount(): Long

    fun missCount(): Long

    fun capacity(): Int

    fun timeout(): Long

    fun setListener(listener: CacheListener<K, V>?): Cache<K, V> = this

    fun setLoader(listener: CacheLoader<K, V>?): Cache<K, V> = this
}
