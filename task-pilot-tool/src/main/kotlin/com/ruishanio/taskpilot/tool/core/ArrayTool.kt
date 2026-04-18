package com.ruishanio.taskpilot.tool.core

/**
 * 数组工具。
 * 继续保留最小化的空判断和查找能力，避免上层代码为了简单数组判断引入额外依赖。
 */
object ArrayTool {
    const val INDEX_NOT_FOUND: Int = -1
    fun isEmpty(array: Array<out Any?>?): Boolean = array == null || array.isEmpty()
    fun isNotEmpty(array: Array<out Any?>?): Boolean = !isEmpty(array)
    fun contains(array: Array<out Any?>?, obj: Any?): Boolean = indexOf(array, obj) != INDEX_NOT_FOUND
    fun indexOf(array: Array<out Any?>?, obj: Any?): Int {
        if (isEmpty(array) || obj == null) {
            return INDEX_NOT_FOUND
        }
        for (i in array!!.indices) {
            if (obj == array[i]) {
                return i
            }
        }
        return INDEX_NOT_FOUND
    }
}
