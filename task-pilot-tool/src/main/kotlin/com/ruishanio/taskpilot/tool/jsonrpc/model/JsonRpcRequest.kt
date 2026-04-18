package com.ruishanio.taskpilot.tool.jsonrpc.model

import com.google.gson.JsonElement
import java.io.Serializable

/**
 * JSON-RPC 请求体。
 */
data class JsonRpcRequest(
    var service: String? = null,
    var method: String? = null,
    var params: Array<JsonElement>? = null
) : Serializable {
    /**
     * 数组字段继续显式展开，避免默认 `toString()` 打出 JVM 地址。
     */
    override fun toString(): String =
        "JsonRpcRequest(service=$service, method=$method, params=${params?.contentDeepToString()})"

    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
