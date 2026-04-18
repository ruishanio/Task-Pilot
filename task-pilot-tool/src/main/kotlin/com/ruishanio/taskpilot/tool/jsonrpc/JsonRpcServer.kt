package com.ruishanio.taskpilot.tool.jsonrpc

import com.google.gson.JsonElement
import com.ruishanio.taskpilot.tool.core.MapTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.jsonrpc.model.JsonRpcRequest
import com.ruishanio.taskpilot.tool.jsonrpc.model.JsonRpcResponse
import com.ruishanio.taskpilot.tool.jsonrpc.model.JsonRpcResponseError
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/**
 * JSON-RPC 服务端分发器。
 * 继续使用最直接的“按服务名取实例、按方法名线性匹配”策略，不在迁移时重写调度规则。
 */
class JsonRpcServer {
    private val serviceStore: MutableMap<String, Any> = ConcurrentHashMap()

    fun register(service: String, serviceInstance: Any): JsonRpcServer {
        serviceStore[service] = serviceInstance
        return this
    }

    /**
     * 批量注册仍按逐项复用单个注册入口处理，保证覆盖语义一致。
     */
    fun register(initServiceStore: Map<String, Any>?): JsonRpcServer {
        if (MapTool.isEmpty(initServiceStore)) {
            return this
        }

        for ((key, value) in initServiceStore!!) {
            register(key, value)
        }
        return this
    }

    fun invoke(requestBody: String?): String {
        val jsonRpcRequest = GsonTool.fromJson(requestBody, JsonRpcRequest::class.java)
        val jsonRpcResponse = doInvoke(jsonRpcRequest)
        return GsonTool.toJson(jsonRpcResponse)
    }

    /**
     * 请求参数数量不匹配、服务未找到、方法未找到都继续返回协议错误体，而不是直接抛异常。
     */
    private fun doInvoke(request: JsonRpcRequest?): JsonRpcResponse {
        val service = request?.service
        val method = request?.method
        val params = request?.params

        try {
            val serviceInstance = serviceStore[service]
                ?: return JsonRpcResponse.ofError(
                    JsonRpcResponseError.SERVICE_NOT_FOUND,
                    "service[$service] not found."
                )

            var methodObj: Method? = null
            for (candidate in serviceInstance.javaClass.methods) {
                if (candidate.name == method) {
                    methodObj = candidate
                    break
                }
            }
            if (methodObj == null) {
                return JsonRpcResponse.ofError(
                    JsonRpcResponseError.METHOD_NOT_FOUND,
                    "method [$method] not found."
                )
            }

            var parameters: Array<Any?>? = null
            val parameterTypes = methodObj.parameterTypes
            if (parameterTypes.isNotEmpty()) {
                if (params == null || params.size != parameterTypes.size) {
                    return JsonRpcResponse.ofError(
                        JsonRpcResponseError.REQUEST_PARAM_ERROR,
                        "method[$method] params number not match."
                    )
                }

                parameters = arrayOfNulls(parameterTypes.size)
                for (i in parameterTypes.indices) {
                    parameters[i] = GsonTool.fromJsonElement(params[i], parameterTypes[i])
                }
            }

            methodObj.isAccessible = true
            val resultObj = methodObj.invoke(serviceInstance, *(parameters ?: emptyArray()))
            val resultJson: JsonElement = GsonTool.toJsonElement(resultObj)
            return JsonRpcResponse.ofSuccess(resultJson)
        } catch (e: Exception) {
            logger.error("JSON-RPC 服务调用时发生异常。", e)
            return JsonRpcResponse.ofError("server invoke error: " + e.message)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JsonRpcServer::class.java)
        fun newServer(): JsonRpcServer = JsonRpcServer()
    }
}
