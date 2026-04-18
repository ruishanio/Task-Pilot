package com.ruishanio.taskpilot.admin.auth.interceptor

import com.ruishanio.taskpilot.admin.auth.annotation.TaskPilotAuth
import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import com.ruishanio.taskpilot.admin.auth.exception.TaskPilotAuthException
import com.ruishanio.taskpilot.admin.auth.helper.TaskPilotAuthHelper
import com.ruishanio.taskpilot.tool.core.StringTool
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.util.AntPathMatcher
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

/**
 * 管理端本地认证拦截器。
 *
 * 页面请求按老系统习惯跳登录页，JSON 请求则统一抛业务码，兼容后台模板和前端 SPA 两种入口。
 */
class TaskPilotAuthInterceptor(
    private val excludedPaths: String?,
    loginPath: String?
) : HandlerInterceptor {
    private val antPathMatcher = AntPathMatcher()
    private val loginPath: String = if (StringTool.isBlank(loginPath)) AuthConst.LOGIN_URL else loginPath!!

    init {
        logger.info("TaskPilotAuthInterceptor initialized.")
    }

    /**
     * 公开排除规则判断，便于测试和后续配置诊断复用。
     */
    fun isMatchExcludedPaths(request: HttpServletRequest): Boolean {
        if (StringTool.isBlank(excludedPaths)) {
            return false
        }

        val servletPath = request.servletPath
        for (excludedPath in excludedPaths!!.split(",")) {
            val uriPattern = excludedPath.trim()
            if (StringTool.isBlank(uriPattern)) {
                continue
            }
            if (antPathMatcher.match(uriPattern, servletPath)) {
                return true
            }
        }
        return false
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // 预检请求不带登录 Cookie，直接交给 CORS 处理。
        if ("OPTIONS".equals(request.method, ignoreCase = true)) {
            return true
        }
        if (handler !is HandlerMethod) {
            return true
        }

        val taskPilotAuth = handler.getMethodAnnotation(TaskPilotAuth::class.java)
        val needLogin = taskPilotAuth?.login ?: true
        val permission = taskPilotAuth?.permission
        val role = taskPilotAuth?.role

        if (isMatchExcludedPaths(request) || !needLogin) {
            return true
        }

        val loginCheckResult = TaskPilotAuthHelper.loginCheckWithCookie(request, response)
        val loginInfo = if (loginCheckResult.isSuccess) loginCheckResult.data else null
        if (loginInfo == null) {
            if (isJsonHandler(handler)) {
                throw TaskPilotAuthException(AuthConst.CODE_LOGIN_FAIL, "not login for path:${request.servletPath}")
            }

            response.sendRedirect("${request.contextPath}$loginPath")
            return false
        }

        request.setAttribute(AuthConst.TASK_PILOT_LOGIN_USER, loginInfo)
        if (!TaskPilotAuthHelper.hasPermission(loginInfo, permission).isSuccess) {
            throw TaskPilotAuthException(AuthConst.CODE_PERMISSION_FAIL, "permission limit, current login-user does not have permission:$permission")
        }
        if (!TaskPilotAuthHelper.hasRole(loginInfo, role).isSuccess) {
            throw TaskPilotAuthException(AuthConst.CODE_ROLE_FAIL, "permission limit, current login-user does not have role:$role")
        }
        return true
    }

    /**
     * 同时兼容 `@ResponseBody` 方法、`@RestController` 类和组合注解场景。
     */
    private fun isJsonHandler(handlerMethod: HandlerMethod): Boolean =
        AnnotatedElementUtils.hasAnnotation(handlerMethod.method, ResponseBody::class.java) ||
            AnnotatedElementUtils.hasAnnotation(handlerMethod.beanType, ResponseBody::class.java)

    companion object {
        private val logger = LoggerFactory.getLogger(TaskPilotAuthInterceptor::class.java)
    }
}
