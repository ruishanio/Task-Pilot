package com.ruishanio.taskpilot.tool.core

import java.text.MessageFormat

/**
 * 字符串工具。
 * 仍以 `object` 形式集中常用字符串处理，避免公共逻辑散落成零碎扩展函数。
 */
object StringTool {
    const val EMPTY: String = ""
    const val INDEX_NOT_FOUND: Int = -1
    fun isEmpty(str: String?): Boolean = str == null || str.isEmpty()
    fun isNotEmpty(str: String?): Boolean = !isEmpty(str)
    fun isBlank(str: String?): Boolean = str == null || str.trim().isEmpty()
    fun isNotBlank(str: String?): Boolean = !isBlank(str)
    fun trim(str: String?): String? = str?.trim()
    fun trimToNull(str: String?): String? {
        val ts = trim(str)
        return if (isEmpty(ts)) null else ts
    }
    fun trimToEmpty(str: String?): String = str?.trim() ?: EMPTY
    fun isNumeric(str: String?): Boolean {
        if (isBlank(str)) {
            return false
        }
        for (ch in str!!.toCharArray()) {
            if (!Character.isDigit(ch)) {
                return false
            }
        }
        return true
    }
    fun countMatches(str: String?, sub: String?): Int {
        if (isEmpty(str) || isEmpty(sub)) {
            return 0
        }
        var count = 0
        var idx = 0
        while (true) {
            idx = str!!.indexOf(sub!!, idx)
            if (idx == INDEX_NOT_FOUND) {
                break
            }
            count++
            idx += sub.length
        }
        return count
    }
    fun upperCaseFirst(text: String?): String? {
        if (isBlank(text)) {
            return text
        }
        return text!!.substring(0, 1).uppercase() + text.substring(1)
    }
    fun lowerCaseFirst(text: String?): String? {
        if (isBlank(text)) {
            return text
        }
        return text!!.substring(0, 1).lowercase() + text.substring(1)
    }

    /**
     * 下划线转驼峰继续采用简单状态机，不额外处理连字符或连续下划线的特殊语义。
     */
    fun underlineToCamelCase(underscoreText: String?): String? {
        if (isBlank(underscoreText)) {
            return underscoreText
        }
        val result = StringBuilder()
        var flag = false
        for (ch in underscoreText!!.toCharArray()) {
            if (ch == '_') {
                flag = true
            } else {
                if (flag) {
                    result.append(ch.uppercaseChar())
                    flag = false
                } else {
                    result.append(ch)
                }
            }
        }
        return result.toString()
    }
    fun substring(str: String?, start: Int): String? {
        if (str == null) {
            return null
        }
        var finalStart = start
        if (finalStart < 0) {
            finalStart = 0
        }
        if (finalStart > str.length) {
            return EMPTY
        }
        return str.substring(finalStart)
    }
    fun substring(str: String?, start: Int, end: Int): String? {
        if (str == null) {
            return null
        }
        var finalStart = start
        var finalEnd = end
        if (finalStart < 0) {
            finalStart = 0
        }
        if (finalStart > str.length) {
            return EMPTY
        }
        if (finalEnd > str.length) {
            finalEnd = str.length
        }
        if (finalStart > finalEnd) {
            return EMPTY
        }
        return str.substring(finalStart, finalEnd)
    }
    fun split(str: String?, separator: String?): List<String>? = split(str, separator, true, true)

    /**
     * split 默认仍会 trim token 并忽略空白项，保持旧工具链的数据清洗习惯。
     */
    fun split(str: String?, separator: String?, trimTokens: Boolean, ignoreBlackTokens: Boolean): List<String>? {
        if (isBlank(str)) {
            return null
        }
        if (isBlank(separator)) {
            return listOf(str!!.trim())
        }

        val list = ArrayList<String>()
        for (itemOrigin in str!!.split(separator!!)) {
            var item = itemOrigin
            if (trimTokens) {
                item = item.trim()
            }
            if (ignoreBlackTokens && isBlank(item)) {
                continue
            }
            list.add(item)
        }
        return list
    }
    fun join(list: List<String?>?, separator: String?): String? = join(list, separator, true, true)
    fun join(list: List<String?>?, separator: String?, trimTokens: Boolean, ignoreBlackTokens: Boolean): String? {
        if (CollectionTool.isEmpty(list)) {
            return null
        }
        val finalSeparator = separator ?: EMPTY
        val buf = StringBuilder()
        var first = true
        for (item in list!!) {
            if (ignoreBlackTokens && isBlank(item)) {
                continue
            }
            val token = if (trimTokens) item!!.trim() else item
            if (first) {
                first = false
            } else {
                buf.append(finalSeparator)
            }
            buf.append(token)
        }
        return buf.toString()
    }
    fun format(template: String, vararg params: Any?): String = MessageFormat.format(template, *params)

    /**
     * 按占位符键名逐个替换，继续跳过值为 `null` 的项，避免把模板占位符误替换成 `"null"`。
     */
    fun formatWithMap(template: String?, params: Map<String, Any?>?): String? {
        if (isBlank(template) || MapTool.isEmpty(params)) {
            return template
        }
        var finalTemplate = template
        for ((key, value) in params!!) {
            if (value == null) {
                continue
            }
            val oldPattern = "{$key}"
            val newPattern = value.toString()
            finalTemplate = replace(finalTemplate, oldPattern, newPattern)
        }
        return finalTemplate
    }
    fun replace(inString: String?, oldPattern: String?, newPattern: String?): String? {
        if (isEmpty(inString) || isEmpty(oldPattern) || newPattern == null) {
            return inString
        }
        return inString!!.replace(oldPattern!!, newPattern)
    }
    fun removePrefix(str: String?, prefix: String?): String? {
        if (str == null || isBlank(prefix)) {
            return str
        }
        return if (str.startsWith(prefix!!)) str.substring(prefix.length) else str
    }
    fun removeSuffix(str: String?, suffix: String?): String? {
        if (str == null || isBlank(suffix)) {
            return str
        }
        return if (str.endsWith(suffix!!)) str.substring(0, str.length - suffix.length) else str
    }
    fun equals(str1: String?, str2: String?): Boolean = if (str1 == null) str2 == null else str1 == str2
}
