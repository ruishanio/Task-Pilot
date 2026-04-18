package com.ruishanio.taskpilot.tool.response

import java.io.Serializable

/**
 * 通用响应体。
 * 这里改成 Kotlin 主构造器 + `data class`，让它回到纯数据载体的写法。
 */
data class Response<T>(
    var code: Int = 0,
    var msg: String? = null,
    var data: T? = null
) : Serializable {
    /**
     * 成功与否继续只看响应码，不把 `data` 是否为空卷进语义里。
     */
    val isSuccess: Boolean
        get() = code == ResponseCode.SUCCESS.code

    companion object {
        private const val serialVersionUID: Long = 42L

        fun isSuccess(response: Response<*>?): Boolean = response?.isSuccess == true

        fun <T> of(code: Int, msg: String?, data: T?): Response<T> = Response(code, msg, data)

        fun <T> of(code: Int, msg: String?): Response<T> = Response(code, msg, null)

        fun <T> ofSuccess(data: T?): Response<T> = Response(ResponseCode.SUCCESS.code, ResponseCode.SUCCESS.msg, data)

        fun <T> ofSuccess(): Response<T> = Response(ResponseCode.SUCCESS.code, ResponseCode.SUCCESS.msg, null)

        fun <T> ofFail(msg: String?): Response<T> = Response(ResponseCode.FAILURE.code, msg, null)

        fun <T> ofFail(): Response<T> = Response(ResponseCode.FAILURE.code, ResponseCode.FAILURE.msg, null)
    }
}
