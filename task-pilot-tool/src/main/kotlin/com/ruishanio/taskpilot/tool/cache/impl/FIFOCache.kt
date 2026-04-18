package com.ruishanio.taskpilot.tool.cache.impl

import com.ruishanio.taskpilot.tool.cache.model.CacheObject
import java.util.LinkedHashMap

/**
 * FIFO 缓存。
 * 容量打满时先清理全部过期对象，仍然超限则删除最早进入的缓存项。
 */
class FIFOCache<K, V>(capacity: Int, timeout: Long, expireType: Boolean) : ReentrantCache<K, V>() {
    init {
        require(capacity > 0) { "capacity must large than 0" }

        val finalCapacity = if (capacity == Int.MAX_VALUE) capacity - 1 else capacity
        this.capacity = finalCapacity
        this.timeout = timeout
        this.expireType = expireType
        cacheMap = LinkedHashMap(finalCapacity + 1, 1.0f, false)
    }

    override fun doPrune(): Int {
        var count = 0
        var first: CacheObject<K, V>? = null
        val values = cacheMap.values.iterator()
        while (values.hasNext()) {
            val cacheObject = values.next()
            if (cacheObject.isExpired()) {
                values.remove()
                onRemove(cacheObject)
                count++
                continue
            }
            if (first == null) {
                first = cacheObject
            }
        }

        if (isFull() && first != null) {
            removeWithoutLock(first.key)
            onRemove(first)
            count++
        }
        return count
    }

    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
