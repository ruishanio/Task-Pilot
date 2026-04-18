package com.ruishanio.taskpilot.tool.http.client

import java.lang.annotation.Inherited

/**
 * HTTP 客户端方法级注解。
 * 继续只承载路径和超时两个维度，避免代理解析逻辑被注解设计反向绑死。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class HttpClientMethod(
    val path: String = "",
    val timeout: Int = -1
)
