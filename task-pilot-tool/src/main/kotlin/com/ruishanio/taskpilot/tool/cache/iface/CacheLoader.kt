package com.ruishanio.taskpilot.tool.cache.iface

/**
 * 缓存加载器，在缓存缺失或过期时按需回填对象。
 */
abstract class CacheLoader<K, V> {
    @Throws(Exception::class)
    abstract fun load(key: K): V
}
