package com.ruishanio.taskpilot.tool.cache.iface

/**
 * 缓存移除监听器，异常继续向上抛出，由缓存实现决定记录或吞并策略。
 */
abstract class CacheListener<K, V> {
    @Throws(Exception::class)
    abstract fun onRemove(key: K, value: V)
}
