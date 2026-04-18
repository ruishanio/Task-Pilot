package com.ruishanio.taskpilot.tool.http.client

import java.lang.annotation.Inherited

/**
 * HTTP 客户端服务级注解。
 * 服务端点的基础地址、路径和默认超时继续集中在类型上声明，兼容现有注解式代理定义。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class HttpClientService(
    val url: String = "",
    val path: String = "",
    val timeout: Int = -1
)
