package com.ruishanio.taskpilot.tool.core

/**
 * 断言工具。
 * 继续统一抛 `IllegalArgumentException`，保持历史调用方依赖的异常类型不变。
 */
object AssertTool {
    fun isTrue(expression: Boolean, message: String) {
        if (!expression) {
            throw IllegalArgumentException(message)
        }
    }
    fun isFalse(expression: Boolean, message: String) {
        isTrue(!expression, message)
    }
    fun isNull(obj: Any?, message: String) {
        if (obj != null) {
            throw IllegalArgumentException(message)
        }
    }
    fun notNull(obj: Any?, message: String) {
        if (obj == null) {
            throw IllegalArgumentException(message)
        }
    }
    fun isBlank(str: String?, message: String) {
        if (StringTool.isNotBlank(str)) {
            throw IllegalArgumentException(message)
        }
    }
    fun notBlank(str: String?, message: String) {
        if (StringTool.isBlank(str)) {
            throw IllegalArgumentException(message)
        }
    }
}
