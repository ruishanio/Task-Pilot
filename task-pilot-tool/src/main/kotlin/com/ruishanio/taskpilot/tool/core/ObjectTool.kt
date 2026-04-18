package com.ruishanio.taskpilot.tool.core

import java.util.StringJoiner

/**
 * 对象工具。
 * 数组字符串化仍统一输出 `{a, b}` 风格，而不是 Kotlin 默认的 `[]`，避免日志格式变化。
 */
object ObjectTool {
    private const val EMPTY_STRING = ""
    private const val NULL_STRING = "null"
    private const val ARRAY_START = "{"
    private const val ARRAY_END = "}"
    private const val EMPTY_ARRAY = ARRAY_START + ARRAY_END
    private const val ARRAY_ELEMENT_SEPARATOR = ", "
    fun equal(a: Any?, b: Any?): Boolean = a === b || (a != null && a == b)
    fun isArray(obj: Any?): Boolean = obj != null && obj.javaClass.isArray
    fun isEmpty(array: Array<out Any?>?): Boolean = array == null || array.isEmpty()
    fun toString(obj: Any?): String {
        if (obj == null) {
            return NULL_STRING
        }
        return when (obj) {
            is String -> obj
            is Array<*> -> toString(obj)
            is BooleanArray -> toDelimitedString(obj.toTypedArray())
            is ByteArray -> toDelimitedString(obj.toTypedArray())
            is CharArray -> toDelimitedString(obj.toTypedArray())
            is DoubleArray -> toDelimitedString(obj.toTypedArray())
            is FloatArray -> toDelimitedString(obj.toTypedArray())
            is IntArray -> toDelimitedString(obj.toTypedArray())
            is LongArray -> toDelimitedString(obj.toTypedArray())
            is ShortArray -> toDelimitedString(obj.toTypedArray())
            else -> obj.toString()
        }
    }
    fun toString(array: Array<out Any?>?): String {
        if (array == null) {
            return NULL_STRING
        }
        if (array.isEmpty()) {
            return EMPTY_ARRAY
        }
        val joiner = StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END)
        for (item in array) {
            joiner.add(item.toString())
        }
        return joiner.toString()
    }

    private fun toDelimitedString(array: Array<out Any?>): String {
        if (array.isEmpty()) {
            return EMPTY_ARRAY
        }
        val joiner = StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END)
        for (item in array) {
            joiner.add(item.toString())
        }
        return joiner.toString()
    }
    fun identityToString(obj: Any?): String {
        if (obj == null) {
            return EMPTY_STRING
        }
        return obj.javaClass.name + "@" + getIdentityHexString(obj)
    }
    fun getIdentityHexString(obj: Any): String = Integer.toHexString(System.identityHashCode(obj))
}
