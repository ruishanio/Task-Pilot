package com.ruishanio.taskpilot.tool.cache.model

import java.io.Serializable
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

/**
 * 缓存条目对象，保留访问时间、创建时间和访问次数三个维度，供不同淘汰策略复用。
 */
class CacheObject<K, V>(
    val key: K,
    val value: V,
    val ttl: Long,
    private val expireType: Boolean
) : Serializable {
    @Volatile
    private var lastAccess: Long = System.currentTimeMillis()

    @Volatile
    private var createTime: Long = System.currentTimeMillis()

    private val accessCount = AtomicLong()

    fun getExpiredTime(): Date? = if (ttl > 0) Date(lastAccess + ttl) else null

    fun getLastAccess(): Long = lastAccess

    /**
     * 过期判断继续沿用旧逻辑，哪怕注释含义与实现命名略有历史包袱，也不在迁移时改行为。
     */
    fun isExpired(): Boolean {
        if (ttl > 0) {
            return if (expireType) {
                (System.currentTimeMillis() - createTime) > ttl
            } else {
                (System.currentTimeMillis() - lastAccess) > ttl
            }
        }
        return false
    }

    fun get(isUpdateLastAccess: Boolean): V {
        if (isUpdateLastAccess) {
            lastAccess = System.currentTimeMillis()
        }
        accessCount.incrementAndGet()
        return value
    }

    fun getAccessCount(): AtomicLong = accessCount

    override fun toString(): String =
        "CacheObject{key=$key, value=$value, lastAccess=$lastAccess, createTime=$createTime, accessCount=$accessCount, ttl=$ttl}"

    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
