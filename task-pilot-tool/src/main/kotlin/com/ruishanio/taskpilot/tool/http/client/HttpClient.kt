package com.ruishanio.taskpilot.tool.http.client

import com.ruishanio.taskpilot.tool.core.MapTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.http.http.HttpRequest
import com.ruishanio.taskpilot.tool.http.http.enums.ContentType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.util.HashMap
import java.util.Objects

/**
 * HTTP 客户端代理。
 * 继续沿用动态代理 + 注解解析的实现，确保现有 `Biz` 接口无需改造即可复用。
 */
class HttpClient {
    private var url: String? = null
    private var headers: MutableMap<String, String>? = null
    private var cookies: MutableMap<String, String>? = null
    private var timeout: Int = 3 * 1000
    private var auth: String? = null

    fun url(url: String?): HttpClient {
        this.url = url
        return this
    }

    /**
     * 整体设置请求头时继续走“清空再覆盖”策略，保持链式配置的后写优先语义。
     */
    fun header(header: Map<String, String>?): HttpClient {
        if (MapTool.isEmpty(header)) {
            return this
        }

        if (MapTool.isNotEmpty(headers)) {
            headers!!.clear()
        }
        for (key in header!!.keys) {
            header(key, header[key])
        }
        return this
    }

    fun header(key: String?, value: String?): HttpClient {
        if (StringTool.isBlank(key) || Objects.isNull(value)) {
            return this
        }

        if (headers == null) {
            headers = HashMap()
        }
        headers!![key!!] = value!!
        return this
    }

    fun cookie(cookie: Map<String, String>?): HttpClient {
        if (MapTool.isEmpty(cookie)) {
            return this
        }

        if (MapTool.isNotEmpty(cookies)) {
            cookies!!.clear()
        }
        for (key in cookie!!.keys) {
            cookie(key, cookie[key])
        }
        return this
    }

    fun cookie(key: String?, value: String?): HttpClient {
        if (StringTool.isBlank(key) || Objects.isNull(value)) {
            return this
        }

        if (cookies == null) {
            cookies = HashMap()
        }
        cookies!![key!!] = value!!
        return this
    }

    fun timeout(timeout: Int): HttpClient {
        this.timeout = timeout
        return this
    }

    fun auth(auth: String?): HttpClient {
        this.auth = auth
        return this
    }

    /**
     * 代理对象继续拦截 `Object` 基础方法，避免业务接口未声明这些方法时走到远程调用分支。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> proxy(serviceInterface: Class<T>): T {
        return Proxy.newProxyInstance(
            serviceInterface.classLoader,
            arrayOf(serviceInterface)
        ) { _, method, args ->
            val methodName = method.name
            val parameterTypes = method.parameterTypes
            if (parameterTypes.isEmpty()) {
                when (methodName) {
                    "toString" -> return@newProxyInstance this.toString()
                    "hashCode" -> return@newProxyInstance this.hashCode()
                }
            } else if (parameterTypes.size == 1 && methodName == "equals") {
                return@newProxyInstance this.equals(args?.get(0))
            }

            invoke(serviceInterface, method, args)
        } as T
    }

    /**
     * 从接口注解、方法注解和客户端默认配置中归并出最终请求。
     * 这里保留原来的优先级顺序和 URL 拼接规则，不在迁移阶段改协议约定。
     */
    private fun invoke(serviceInterface: Class<*>, method: Method, params: Array<Any?>?): Any? {
        val httpClientService = serviceInterface.getAnnotation(HttpClientService::class.java)
        val httpClientMethod = method.getAnnotation(HttpClientMethod::class.java)
        val responseType = method.genericReturnType

        var baseUrl = url
        if (StringTool.isBlank(baseUrl)) {
            if (httpClientService != null && StringTool.isNotBlank(httpClientService.url)) {
                baseUrl = httpClientService.url
            }
        }
        if (StringTool.isBlank(baseUrl)) {
            throw RuntimeException("http client invoke fail, baseUrl is null")
        }
        baseUrl = StringTool.removeSuffix(baseUrl, "/")

        val servicePathFinal = parseServicePath(httpClientService, serviceInterface)
        val methodPathFinal = parseMethodPath(httpClientMethod, method)
        val finalUrl =
            if (StringTool.isNotBlank(servicePathFinal)) {
                "$baseUrl/$servicePathFinal/$methodPathFinal"
            } else {
                "$baseUrl/$methodPathFinal"
            }

        val methodTimeout =
            when {
                httpClientMethod != null && httpClientMethod.timeout > 0 -> httpClientMethod.timeout
                httpClientService != null && httpClientService.timeout > 0 -> httpClientService.timeout
                timeout > 0 -> timeout
                else -> -1
            }
        if (methodTimeout <= 0) {
            throw RuntimeException("http client invoke fail, timeout invalid")
        }

        val request =
            if (params != null) {
                if (params.size == 1) params[0] else params
            } else {
                null
            }

        val httpRequest =
            HttpTool.createPost(finalUrl)
                .contentType(ContentType.JSON)
                .header(headers)
                .cookie(cookies)
                .connectTimeout(methodTimeout)
                .readTimeout(methodTimeout)
                .useCaches(false)
                .auth(auth)
                .request(request)

        /**
         * 先把反射类型收敛成运行期可判定的 `Class`，避免在 Kotlin 侧留下未经检查的泛型强转告警。
         */
        return when (responseType) {
            is ParameterizedType -> {
                val rawType = responseType.rawType
                require(rawType is Class<*>) { "responseType rawType is not a Class: $rawType" }
                httpRequest.execute().response(rawType, *responseType.actualTypeArguments)
            }
            Void.TYPE -> {
                httpRequest.execute()
                null
            }
            is Class<*> -> httpRequest.execute().response(responseType)
            else -> throw IllegalArgumentException("Unsupported responseType: $responseType")
        }
    }

    companion object {
        /**
         * 服务路径优先使用注解值，并保留首尾 `/` 清洗逻辑，兼容旧配置写法。
         */
        fun parseServicePath(httpRpcService: HttpClientService?, serviceInterface: Class<*>): String? {
            var servicePathFinal: String? = null
            if (httpRpcService != null && StringTool.isNotBlank(httpRpcService.path)) {
                servicePathFinal = httpRpcService.path
            }

            servicePathFinal = StringTool.removePrefix(servicePathFinal, "/")
            servicePathFinal = StringTool.removeSuffix(servicePathFinal, "/")
            return servicePathFinal
        }

        /**
         * 方法路径优先使用注解值，否则回退到方法名。
         */
        fun parseMethodPath(httpRpcMethod: HttpClientMethod?, method: Method): String {
            var methodPath =
                if (httpRpcMethod != null && StringTool.isNotBlank(httpRpcMethod.path)) {
                    httpRpcMethod.path
                } else {
                    method.name
                }

            methodPath = StringTool.removePrefix(methodPath, "/")
            methodPath = StringTool.removeSuffix(methodPath, "/")
            return methodPath
        }
    }
}
