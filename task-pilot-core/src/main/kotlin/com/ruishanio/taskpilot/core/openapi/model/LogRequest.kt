package com.ruishanio.taskpilot.core.openapi.model

import java.io.Serializable

/**
 * 日志拉取请求。
 */
data class LogRequest(
    var logDateTim: Long = 0,
    var logId: Long = 0,
    var fromLineNum: Int = 0
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
