package com.ruishanio.taskpilot.tool.cache.model

import java.io.Serializable
import java.util.Objects

/**
 * 缓存键包装器，继续只按内部 key 判等，避免缓存容器在迁移后命中语义发生变化。
 */
class CacheKey<K> : Serializable {
    private var key: K? = null

    constructor(key: K?) {
        this.key = key
    }

    fun get(): K? = key

    fun set(key: K?) {
        this.key = key
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (this === other) {
            return true
        }
        if (javaClass == other.javaClass) {
            val that = other as CacheKey<*>
            return Objects.equals(key, that.key)
        }
        return false
    }

    override fun hashCode(): Int = key?.hashCode() ?: 0

    override fun toString(): String = key?.toString() ?: "null"

    companion object {
        private const val serialVersionUID: Long = 42L
        fun <K> of(key: K?): CacheKey<K> = CacheKey(key)
    }
}
