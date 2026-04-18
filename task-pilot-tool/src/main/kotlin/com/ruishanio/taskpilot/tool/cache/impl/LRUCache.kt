package com.ruishanio.taskpilot.tool.cache.impl

import com.ruishanio.taskpilot.tool.cache.model.CacheKey
import com.ruishanio.taskpilot.tool.cache.model.CacheObject
import java.util.LinkedHashMap
import org.slf4j.LoggerFactory

/**
 * LRU 缓存。
 * 清理过期对象之外，容量淘汰直接交给按访问顺序维护的 LinkedHashMap。
 */
class LRUCache<K, V>(capacity: Int, timeout: Long, expireType: Boolean) : ReentrantCache<K, V>() {
    init {
        require(capacity > 0) { "capacity must large than 0" }

        val finalCapacity = if (capacity == Int.MAX_VALUE) capacity - 1 else capacity
        this.capacity = finalCapacity
        this.timeout = timeout
        this.expireType = expireType
        cacheMap = object : LinkedHashMap<CacheKey<K>, CacheObject<K, V>>(finalCapacity + 1, 1.0f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey<K>, CacheObject<K, V>>): Boolean {
                if (size > finalCapacity) {
                    if (listener != null) {
                        try {
                            listener!!.onRemove(eldest.value.key, eldest.value.value)
                        } catch (e: Exception) {
                            logger.error("执行缓存移除监听器时发生异常，cacheObject:{}", eldest, e)
                        }
                    }
                    return true
                }
                return false
            }
        }
    }

    override fun doPrune(): Int {
        var count = 0
        val values = cacheMap.values.iterator()
        while (values.hasNext()) {
            val cacheObject = values.next()
            if (cacheObject.isExpired()) {
                values.remove()
                onRemove(cacheObject)
                count++
            }
        }
        return count
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LRUCache::class.java)
        private const val serialVersionUID: Long = 42L
    }
}
