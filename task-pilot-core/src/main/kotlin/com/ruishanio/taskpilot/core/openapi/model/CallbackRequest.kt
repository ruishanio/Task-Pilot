package com.ruishanio.taskpilot.core.openapi.model

import java.io.Serializable

/**
 * 执行结果回调请求。
 */
data class CallbackRequest(
    var logId: Long = 0,
    var logDateTim: Long = 0,
    var handleCode: Int = 0,
    var handleMsg: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
