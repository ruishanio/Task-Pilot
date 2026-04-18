package com.ruishanio.taskpilot.tool.http

import com.ruishanio.taskpilot.tool.core.MapTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.client.HttpClient
import com.ruishanio.taskpilot.tool.http.http.HttpRequest
import com.ruishanio.taskpilot.tool.http.http.enums.Method
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * HTTP 工具入口。
 * 保留请求构建、客户端代理和 URL 参数处理三个职责，避免现有调用链分散到多个工具类。
 */
object HttpTool {
    /** 创建基础请求对象。 */
    fun createRequest(): HttpRequest = HttpRequest()

    /** 创建默认 POST 请求对象。 */
    fun createRequest(url: String?): HttpRequest = HttpRequest().url(url).method(Method.POST)

    /** 创建 GET 请求对象。 */
    fun createGet(url: String?): HttpRequest = HttpRequest().url(url).method(Method.GET)

    /** 创建 POST 请求对象。 */
    fun createPost(url: String?): HttpRequest = HttpRequest().url(url).method(Method.POST)

    /** 创建声明式 HTTP 客户端。 */
    fun createClient(): HttpClient = HttpClient()

    /** 检测是否为 HTTPS 地址。 */
    fun isHttps(url: String?): Boolean = url != null && url.lowercase().startsWith("https:")

    /** 检测是否为 HTTP 地址。 */
    fun isHttp(url: String?): Boolean = url != null && url.lowercase().startsWith("http:")

    /**
     * 将参数 Map 拼成查询串。
     * 保持原实现的编码和遍历顺序，不额外对 `null` 值做过滤或替换。
     */
    fun generateUrlParam(map: Map<String, String>?): String? {
        if (MapTool.isEmpty(map)) {
            return null
        }

        val param = StringBuilder()
        for (key in map!!.keys) {
            if (param.isNotEmpty()) {
                param.append("&")
            }
            param.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append("=")
                .append(URLEncoder.encode(map[key], StandardCharsets.UTF_8))
        }
        return param.toString()
    }

    /**
     * 将查询串解析为参数 Map。
     * 继续只处理最简单的 `k=v` 形式，不在迁移时补充 URL decode 等扩展行为。
     */
    fun parseUrlParam(url: String?): MutableMap<String, String> {
        var finalUrl = url
        finalUrl = if (finalUrl != null && finalUrl.contains("?")) {
            finalUrl.substring(finalUrl.indexOf("?") + 1)
        } else {
            finalUrl
        }

        val map = MapTool.newMap<String, String>()
        if (StringTool.isNotBlank(finalUrl)) {
            val params = finalUrl!!.split("&")
            for (param in params) {
                val kv = param.split("=")
                if (kv.size == 2) {
                    map[kv[0]] = kv[1]
                }
            }
        }
        return map
    }
}
