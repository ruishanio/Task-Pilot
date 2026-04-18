package com.ruishanio.taskpilot.tool.jsonrpc.model

import com.google.gson.JsonElement
import java.io.Serializable

/**
 * JSON-RPC 响应体。
 */
data class JsonRpcResponse(
    var error: JsonRpcResponseError? = null,
    var result: JsonElement? = null
) : Serializable {
    val isSuccess: Boolean
        get() = !isError

    val isError: Boolean
        get() = error?.code?.let { it > 0 } == true

    companion object {
        private const val serialVersionUID: Long = 42L

        fun ofSuccess(result: JsonElement?): JsonRpcResponse = JsonRpcResponse(result = result)

        fun ofError(error: String?): JsonRpcResponse =
            JsonRpcResponse(error = JsonRpcResponseError(JsonRpcResponseError.SYSTEM_ERROR, error))

        fun ofError(errorCode: Int, errorMsg: String?): JsonRpcResponse =
            JsonRpcResponse(error = JsonRpcResponseError(errorCode, errorMsg))
    }
}
