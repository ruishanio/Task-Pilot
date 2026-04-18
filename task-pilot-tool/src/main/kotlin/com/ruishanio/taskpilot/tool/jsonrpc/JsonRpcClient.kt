package com.ruishanio.taskpilot.tool.jsonrpc

import com.google.gson.JsonElement
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.HttpTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.jsonrpc.model.JsonRpcRequest
import com.ruishanio.taskpilot.tool.jsonrpc.model.JsonRpcResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.util.HashMap
import java.util.Objects

/**
 * JSON-RPC 客户端。
 * 继续保留 builder + 动态代理两套入口，避免上层同时改调用风格。
 */
class JsonRpcClient {
    private var url: String? = null
    private var timeout: Int = 3000
    private var headers: MutableMap<String, String>? = null

    fun url(url: String?): JsonRpcClient {
        this.url = url
        return this
    }

    fun timeout(timeout: Int): JsonRpcClient {
        this.timeout = timeout
        return this
    }

    /**
     * 头信息支持整体覆盖，保持与原 builder 的“后一次调用覆盖前一次集合”语义一致。
     */
    fun header(headers: Map<String, String>?): JsonRpcClient {
        this.headers = headers?.let { HashMap(it) }
        return this
    }

    fun header(key: String?, value: String?): JsonRpcClient {
        if (StringTool.isBlank(key) || Objects.isNull(value)) {
            return this
        }

        if (headers == null) {
            headers = HashMap()
        }
        headers!![key!!] = value!!
        return this
    }

    fun <T> proxy(serviceInterface: Class<T>): T = proxy(null, serviceInterface)

    /**
     * 代理返回值类型仍按 Java 反射结果推断，保证泛型返回值继续走 `typeArguments` 反序列化分支。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> proxy(service: String?, serviceInterface: Class<T>): T {
        return Proxy.newProxyInstance(
            serviceInterface.classLoader,
            arrayOf(serviceInterface)
        ) { _, method, args ->
            val serviceName = service ?: serviceInterface.name
            val methodName = method.name
            val responseType = method.genericReturnType

            val typeOfResponse: Class<Any>?
            val typeArguments: Array<Type>?
            if (responseType == Void.TYPE) {
                typeOfResponse = null
                typeArguments = null
            } else if (responseType is ParameterizedType) {
                typeOfResponse = responseType.rawType as Class<Any>
                typeArguments = responseType.actualTypeArguments
            } else {
                typeOfResponse = responseType as Class<Any>
                typeArguments = null
            }

            invoke(serviceName, methodName, args, typeOfResponse, typeArguments)
        } as T
    }

    fun <T> invoke(
        service: String?,
        method: String?,
        params: Array<Any?>?,
        responseType: Class<T>?
    ): T? = invoke(service, method, params, responseType, null)

    /**
     * 请求发送和结果解析维持单入口，便于继续复用原有异常包装格式。
     */
    fun <T> invoke(
        service: String?,
        method: String?,
        params: Array<Any?>?,
        responseType: Class<T>?,
        typeArguments: Array<Type>?
    ): T? {
        try {
            val paramJsons =
                params?.map { GsonTool.toJsonElement(it) }?.toTypedArray<JsonElement>()
            val request = JsonRpcRequest(service, method, paramJsons)

            val requestJson = GsonTool.toJson(request)
            val responseData = HttpTool
                .createPost(url)
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .header(headers)
                .body(requestJson)
                .execute()
                .response()

            if (responseData == null || responseData.isEmpty()) {
                throw RuntimeException("response data not found")
            }

            val response = GsonTool.fromJson(responseData, JsonRpcResponse::class.java)
            if (response.isError) {
                throw RuntimeException("invoke error: " + response.error)
            }

            return when {
                responseType == null -> null
                typeArguments != null -> GsonTool.fromJsonElement(response.result, responseType, *typeArguments)
                else -> GsonTool.fromJsonElement(response.result, responseType)
            }
        } catch (e: Throwable) {
            throw RuntimeException("invoke error[2], service:$service, method:$method", e)
        }
    }

    companion object {
        fun newClient(): JsonRpcClient = JsonRpcClient()
    }
}
