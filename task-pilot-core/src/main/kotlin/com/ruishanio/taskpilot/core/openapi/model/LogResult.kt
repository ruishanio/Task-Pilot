package com.ruishanio.taskpilot.core.openapi.model

import java.io.Serializable

/**
 * 日志读取结果。
 */
data class LogResult(
    var fromLineNum: Int = 0,
    var toLineNum: Int = 0,
    var logContent: String? = null,
    var isEnd: Boolean = false
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
