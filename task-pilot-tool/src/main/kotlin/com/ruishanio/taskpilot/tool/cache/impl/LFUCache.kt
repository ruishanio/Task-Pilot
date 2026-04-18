package com.ruishanio.taskpilot.tool.cache.impl

import com.ruishanio.taskpilot.tool.cache.model.CacheObject
import java.util.concurrent.ConcurrentHashMap

/**
 * LFU 缓存。
 * 在容量压力下淘汰访问次数最少的对象，并把其余对象计数整体回拨，避免计数长期膨胀。
 */
class LFUCache<K, V>(capacity: Int, timeout: Long, expireType: Boolean) : ReentrantCache<K, V>() {
    init {
        require(capacity > 0) { "capacity must large than 0" }

        val finalCapacity = if (capacity == Int.MAX_VALUE) capacity - 1 else capacity
        this.capacity = finalCapacity
        this.timeout = timeout
        this.expireType = expireType
        cacheMap = ConcurrentHashMap(finalCapacity + 1, 1.0f)
    }

    override fun doPrune(): Int {
        var count = 0
        var leastVisitedCacheObject: CacheObject<K, V>? = null
        var values = cacheMap.values.iterator()
        while (values.hasNext()) {
            val cacheObject = values.next()
            if (cacheObject.isExpired()) {
                values.remove()
                onRemove(cacheObject)
                count++
                continue
            }
            if (leastVisitedCacheObject == null ||
                cacheObject.getAccessCount().get() < leastVisitedCacheObject.getAccessCount().get()
            ) {
                leastVisitedCacheObject = cacheObject
            }
        }

        if (isFull() && leastVisitedCacheObject != null) {
            removeWithoutLock(leastVisitedCacheObject.key)
            onRemove(leastVisitedCacheObject)
            count++

            val minAccessCount = leastVisitedCacheObject.getAccessCount().get()
            values = cacheMap.values.iterator()
            while (values.hasNext()) {
                val cacheObject = values.next()
                cacheObject.getAccessCount().addAndGet(-minAccessCount)
            }
        }

        return count
    }

    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
