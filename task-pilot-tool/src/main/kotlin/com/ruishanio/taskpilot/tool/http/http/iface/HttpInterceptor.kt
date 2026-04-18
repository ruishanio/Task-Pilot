package com.ruishanio.taskpilot.tool.http.http.iface

import com.ruishanio.taskpilot.tool.http.http.HttpRequest
import com.ruishanio.taskpilot.tool.http.http.HttpResponse

/**
 * HTTP 调用拦截器，分别在请求发送前后开放扩展点。
 */
interface HttpInterceptor {
    fun before(httpRequest: HttpRequest)

    fun after(httpRequest: HttpRequest, httpResponse: HttpResponse)
}
