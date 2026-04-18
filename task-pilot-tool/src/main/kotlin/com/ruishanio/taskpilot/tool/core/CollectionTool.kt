package com.ruishanio.taskpilot.tool.core

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet

/**
 * 集合工具。
 * 并集、交集等运算继续沿用“按元素出现次数”模型，避免迁移后多重集合语义变化。
 */
object CollectionTool {
    private val INTEGER_ONE: Int = Integer.valueOf(1)
    fun isEmpty(coll: kotlin.collections.Collection<*>?): Boolean = coll == null || coll.isEmpty()
    fun isNotEmpty(coll: kotlin.collections.Collection<*>?): Boolean = !isEmpty(coll)
    fun contains(collection: kotlin.collections.Collection<*>?, value: Any?): Boolean =
        isNotEmpty(collection) && collection!!.contains(value)
    fun union(a: kotlin.collections.Collection<*>?, b: kotlin.collections.Collection<*>?): kotlin.collections.Collection<Any?> {
        val list = ArrayList<Any?>()
        val mapa = getCardinalityMap(a ?: emptyList<Any?>())
        val mapb = getCardinalityMap(b ?: emptyList<Any?>())
        val elements = HashSet<Any?>()
        elements.addAll(a ?: emptyList())
        elements.addAll(b ?: emptyList())
        for (obj in elements) {
            val max = maxOf(getFreq(obj, mapa), getFreq(obj, mapb))
            repeat(max) { list.add(obj) }
        }
        return list
    }

    private fun getFreq(obj: Any?, freqMap: kotlin.collections.Map<Any?, Int>): Int = freqMap[obj] ?: 0
    fun getCardinalityMap(coll: kotlin.collections.Collection<*>): kotlin.collections.Map<Any?, Int> {
        val count = HashMap<Any?, Int>()
        for (obj in coll) {
            val c = count[obj]
            if (c == null) {
                count[obj] = INTEGER_ONE
            } else {
                count[obj] = c + 1
            }
        }
        return count
    }
    fun intersection(a: kotlin.collections.Collection<*>?, b: kotlin.collections.Collection<*>?): kotlin.collections.Collection<Any?> {
        val list = ArrayList<Any?>()
        val mapa = getCardinalityMap(a ?: emptyList<Any?>())
        val mapb = getCardinalityMap(b ?: emptyList<Any?>())
        val elements = HashSet<Any?>()
        elements.addAll(a ?: emptyList())
        elements.addAll(b ?: emptyList())
        for (obj in elements) {
            val min = minOf(getFreq(obj, mapa), getFreq(obj, mapb))
            repeat(min) { list.add(obj) }
        }
        return list
    }
    fun disjunction(a: kotlin.collections.Collection<*>?, b: kotlin.collections.Collection<*>?): kotlin.collections.Collection<Any?> {
        val list = ArrayList<Any?>()
        val mapa = getCardinalityMap(a ?: emptyList<Any?>())
        val mapb = getCardinalityMap(b ?: emptyList<Any?>())
        val elements = HashSet<Any?>()
        elements.addAll(a ?: emptyList())
        elements.addAll(b ?: emptyList())
        for (obj in elements) {
            val diff = maxOf(getFreq(obj, mapa), getFreq(obj, mapb)) - minOf(getFreq(obj, mapa), getFreq(obj, mapb))
            repeat(diff) { list.add(obj) }
        }
        return list
    }
    fun subtract(a: kotlin.collections.Collection<*>?, b: kotlin.collections.Collection<*>?): kotlin.collections.Collection<Any?> {
        val list = ArrayList<Any?>()
        if (a != null) {
            list.addAll(a)
        }
        if (b != null) {
            for (item in b) {
                list.remove(item)
            }
        }
        return list
    }
    fun <E> newArrayList(): ArrayList<E> = ArrayList()
    fun <E> newArrayList(vararg elements: E): ArrayList<E> {
        val list = ArrayList<E>()
        Collections.addAll(list, *elements)
        return list
    }

    /**
     * 分批时继续返回原列表的 `subList` 视图，保持历史内存语义和修改联动行为。
     */
    fun <E> split(list: kotlin.collections.List<E>?, batchSize: Int): kotlin.collections.List<kotlin.collections.List<E>> {
        if (list == null || list.isEmpty() || batchSize <= 0) {
            return emptyList()
        }

        val result = ArrayList<kotlin.collections.List<E>>()
        var i = 0
        while (i < list.size) {
            val end = minOf(i + batchSize, list.size)
            result.add(list.subList(i, end))
            i += batchSize
        }
        return result
    }
}
