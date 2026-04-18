package com.ruishanio.taskpilot.tool.cache.impl

import java.util.HashMap

/**
 * 无容量上限缓存。
 * 保持历史实现不因 prune 主动删除数据，只依赖父类读路径和上层定时策略处理。
 */
class UnlimitedCache<K, V>(timeout: Long, expireType: Boolean) : ReentrantCache<K, V>() {
    init {
        this.capacity = 0
        this.timeout = timeout
        this.expireType = expireType
        cacheMap = HashMap()
    }

    override fun doPrune(): Int = 0

    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
