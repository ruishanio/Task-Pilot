package com.ruishanio.taskpilot.tool.jsonrpc.model

import java.io.Serializable

/**
 * JSON-RPC 错误体。
 */
data class JsonRpcResponseError(
    var code: Int = 0,
    var msg: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 42L

        const val SYSTEM_ERROR = 3000
        const val SERVICE_NOT_FOUND = 3001
        const val METHOD_NOT_FOUND = 3002
        const val REQUEST_PARAM_ERROR = 3003
    }
}
