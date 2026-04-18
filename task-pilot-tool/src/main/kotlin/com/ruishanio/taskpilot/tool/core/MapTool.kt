package com.ruishanio.taskpilot.tool.core

import java.text.NumberFormat
import java.text.ParseException
import java.util.HashMap

/**
 * Map 工具。
 * 继续提供宽松的类型读取能力，字符串和数字之间仍允许按历史规则互转。
 */
object MapTool {
    fun isEmpty(map: kotlin.collections.Map<*, *>?): Boolean = map == null || map.isEmpty()
    fun isNotEmpty(map: kotlin.collections.Map<*, *>?): Boolean = !isEmpty(map)
    fun <K> getString(map: kotlin.collections.Map<in K, *>?, key: K): String? = map?.get(key)?.toString()
    fun <K> getBoolean(map: kotlin.collections.Map<in K, *>?, key: K): Boolean? {
        val value = map?.get(key) ?: return null
        return when (value) {
            is Boolean -> value
            is String -> value.toBoolean()
            is Number -> value.toInt() != 0
            else -> null
        }
    }
    fun <K> getNumber(map: kotlin.collections.Map<in K, *>?, key: K): Number? {
        val value = map?.get(key) ?: return null
        return when (value) {
            is Number -> value
            is String ->
                try {
                    NumberFormat.getInstance().parse(value)
                } catch (_: ParseException) {
                    null
                }
            else -> null
        }
    }
    fun <K> getByte(map: kotlin.collections.Map<in K, *>?, key: K): Byte? = getNumber(map, key)?.toByte()
    fun <K> getShort(map: kotlin.collections.Map<in K, *>?, key: K): Short? = getNumber(map, key)?.toShort()
    fun <K> getInteger(map: kotlin.collections.Map<in K, *>?, key: K): Int? = getNumber(map, key)?.toInt()
    fun <K> getLong(map: kotlin.collections.Map<in K, *>?, key: K): Long? = getNumber(map, key)?.toLong()
    fun <K> getFloat(map: kotlin.collections.Map<in K, *>?, key: K): Float? = getNumber(map, key)?.toFloat()
    fun <K> getDouble(map: kotlin.collections.Map<in K, *>?, key: K): Double? = getNumber(map, key)?.toDouble()
    fun <K, V> newMap(): HashMap<K, V> = HashMap()
    fun <K, V> newMap(k1: K, v1: V): HashMap<K, V> =
        HashMap<K, V>().also {
            it[k1] = v1
        }
    fun <K, V> newMap(k1: K, v1: V, k2: K, v2: V): HashMap<K, V> =
        HashMap<K, V>().also {
            it[k1] = v1
            it[k2] = v2
        }
    fun <K, V> newMap(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V): HashMap<K, V> =
        HashMap<K, V>().also {
            it[k1] = v1
            it[k2] = v2
            it[k3] = v3
        }
    fun <K, V> newMap(k1: K, v1: V, k2: K, v2: V, k3: K, v3: V, k4: K, v4: V): HashMap<K, V> =
        HashMap<K, V>().also {
            it[k1] = v1
            it[k2] = v2
            it[k3] = v3
            it[k4] = v4
        }
    fun <K, V> newMap(
        k1: K,
        v1: V,
        k2: K,
        v2: V,
        k3: K,
        v3: V,
        k4: K,
        v4: V,
        k5: K,
        v5: V
    ): HashMap<K, V> =
        HashMap<K, V>().also {
            it[k1] = v1
            it[k2] = v2
            it[k3] = v3
            it[k4] = v4
            it[k5] = v5
        }
}
