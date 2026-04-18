package com.ruishanio.taskpilot.tool.http.http

import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import java.lang.reflect.Type

/**
 * HTTP 响应模型。
 * 保留 Java 时代的属性 setter 与 `statusCode()/response()/cookie()` 访问风格，避免调用方批量改写。
 */
class HttpResponse {
    var statusCode: Int = 0
    var response: String? = null
    var url: String? = null
    var cookies: Map<String, String>? = null

    /**
     * 成功判定继续只看 200，避免迁移阶段顺手扩展状态码语义。
     */
    val isSuccess: Boolean
        get() = statusCode == 200

    fun statusCode(): Int = statusCode

    fun response(): String? = response

    /**
     * 反序列化前先校验状态码和响应体，保持旧版失败路径仍由运行时异常承接。
     */
    fun <T> response(typeOfT: Class<T>?, vararg typeArguments: Type): T? {
        if (!isSuccess) {
            throw RuntimeException("Http Request fail, statusCode($statusCode) for url : $url")
        }
        if (StringTool.isBlank(response) || typeOfT == null) {
            return null
        }

        return if (typeArguments.isNotEmpty()) {
            GsonTool.fromJson(response, typeOfT, *typeArguments)
        } else {
            GsonTool.fromJson(response, typeOfT)
        }
    }

    fun url(): String? = url

    fun cookies(): Map<String, String>? = cookies

    /**
     * Cookie 查询继续对空 Cookie 容器返回 `null`，兼容现有判空逻辑。
     */
    fun cookie(key: String?): String? = cookies?.get(key)
}
