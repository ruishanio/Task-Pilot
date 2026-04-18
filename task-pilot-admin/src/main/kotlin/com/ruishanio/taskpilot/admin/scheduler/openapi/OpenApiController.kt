package com.ruishanio.taskpilot.admin.scheduler.openapi

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.openapi.AdminBiz
import com.ruishanio.taskpilot.core.openapi.model.AutoRegisterRequest
import com.ruishanio.taskpilot.core.openapi.model.CallbackRequest
import com.ruishanio.taskpilot.core.openapi.model.RegistryRequest
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

/**
 * 执行器侧 OpenAPI 入口。
 *
 * 保持单入口分发模式，避免执行器端协议升级时需要同时改动多个 HTTP 路由。
 */
@Controller
class OpenApiController {
    @Resource
    private lateinit var adminBiz: AdminBiz

    /**
     * 统一校验请求方法、token 与请求体，再按 uri 分发到管理接口。
     */
    @RequestMapping("/api/{uri}")
    @ResponseBody
    @TaskPilotAuth(login = false)
    fun api(
        request: HttpServletRequest,
        @PathVariable("uri") uri: String?,
        @RequestHeader(value = Const.TASK_PILOT_ACCESS_TOKEN, required = false) accesstoken: String?,
        @RequestBody(required = false) requestBody: String?
    ): Any {
        if (!"POST".equals(request.method, ignoreCase = true)) {
            return Response.ofFail<String>("invalid request, HttpMethod not support.")
        }
        if (StringTool.isBlank(uri)) {
            return Response.ofFail<String>("invalid request, uri-mapping empty.")
        }
        if (StringTool.isBlank(requestBody)) {
            return Response.ofFail<String>("invalid request, requestBody empty.")
        }

        if (StringTool.isNotBlank(TaskPilotAdminBootstrap.instance.accessToken) &&
            TaskPilotAdminBootstrap.instance.accessToken != accesstoken
        ) {
            return Response.ofFail<String>("The access token is wrong.")
        }

        return try {
            when (uri) {
                "callback" -> {
                    val callbackParamList = GsonTool.fromJsonList(requestBody, CallbackRequest::class.java)
                    adminBiz.callback(callbackParamList)
                }
                "registry" -> {
                    val registryParam: RegistryRequest = GsonTool.fromJson(requestBody, RegistryRequest::class.java)
                    adminBiz.registry(registryParam)
                }
                "registryRemove" -> {
                    val registryParam: RegistryRequest = GsonTool.fromJson(requestBody, RegistryRequest::class.java)
                    adminBiz.registryRemove(registryParam)
                }
                "autoRegister" -> {
                    val autoRegisterRequest: AutoRegisterRequest = GsonTool.fromJson(requestBody, AutoRegisterRequest::class.java)
                    adminBiz.autoRegister(autoRegisterRequest)
                }
                else -> Response.ofFail<String>("invalid request, uri-mapping($uri) not found.")
            }
        } catch (e: Exception) {
            Response.ofFail<String>("openapi invoke error: ${e.message}")
        }
    }
}
