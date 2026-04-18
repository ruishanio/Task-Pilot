package com.ruishanio.taskpilot.admin.auth.helper

import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import com.ruishanio.taskpilot.admin.auth.exception.TaskPilotAuthException
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.auth.store.LoginStore
import com.ruishanio.taskpilot.admin.auth.token.TokenHelper
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.CookieTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * 管理端本地认证辅助对象。
 *
 * 统一管理 cookie 会话和 request 里的已解析登录态，避免控制器重复处理认证细节。
 */
object TaskPilotAuthHelper {
    private lateinit var loginStore: LoginStore
    private var tokenKey: String = AuthConst.TASK_PILOT_LOGIN_TOKEN
    private var tokenTimeout: Long = AuthConst.EXPIRE_TIME_FOR_10_YEAR

    fun init(loginStore: LoginStore, tokenKey: String?, tokenTimeout: Long) {
        this.loginStore = loginStore
        this.tokenKey = if (StringTool.isBlank(tokenKey)) AuthConst.TASK_PILOT_LOGIN_TOKEN else tokenKey!!.trim()
        this.tokenTimeout = if (tokenTimeout <= 0) AuthConst.EXPIRE_TIME_FOR_10_YEAR else tokenTimeout
    }

    /**
     * 所有对外能力都依赖初始化后的登录存储，未初始化直接报错，避免静默放大认证问题。
     */
    private fun ensureInitialized() {
        if (!::loginStore.isInitialized) {
            throw TaskPilotAuthException("task-pilot auth helper not initialized.")
        }
    }

    fun login(loginInfo: LoginInfo?): Response<String> {
        ensureInitialized()
        val currentLoginInfo = loginInfo ?: return Response.ofFail("loginInfo is null")
        currentLoginInfo.expireTime = System.currentTimeMillis() + tokenTimeout
        val tokenResponse = TokenHelper.generateToken(currentLoginInfo)
        if (!tokenResponse.isSuccess) {
            return tokenResponse
        }

        val setResponse = loginStore.set(currentLoginInfo)
        if (!setResponse.isSuccess) {
            return setResponse
        }
        return Response.ofSuccess(tokenResponse.data)
    }

    fun loginWithCookie(
        loginInfo: LoginInfo?,
        response: HttpServletResponse,
        ifRemember: Boolean
    ): Response<String> {
        val loginResult = login(loginInfo)
        if (loginResult.isSuccess) {
            CookieTool.set(response, tokenKey, loginResult.data!!, ifRemember)
        }
        return loginResult
    }

    fun loginUpdate(loginInfo: LoginInfo?): Response<String> {
        ensureInitialized()
        if (loginInfo != null) {
            loginInfo.expireTime = System.currentTimeMillis() + tokenTimeout
        }
        return loginStore.update(loginInfo)
    }

    fun logout(token: String?): Response<String> {
        ensureInitialized()
        val loginInfoForToken = TokenHelper.parseToken(token)
        if (loginInfoForToken == null) {
            return Response.ofFail("token is invalid")
        }
        return loginStore.remove(loginInfoForToken.userId)
    }

    fun logoutWithCookie(request: HttpServletRequest, response: HttpServletResponse): Response<String> {
        ensureInitialized()
        val token = CookieTool.getValue(request, tokenKey)
        if (StringTool.isBlank(token)) {
            return Response.ofSuccess()
        }

        val logoutResult = logout(token)
        CookieTool.remove(request, response, tokenKey)
        return logoutResult
    }

    fun loginCheck(token: String?): Response<LoginInfo> {
        ensureInitialized()
        val loginInfoForToken = TokenHelper.parseToken(token)
        if (loginInfoForToken == null || StringTool.isBlank(loginInfoForToken.signature)) {
            return Response.ofFail("token is invalid")
        }

        val loginInfoResponse = loginStore.get(loginInfoForToken.userId)
        if (!loginInfoResponse.isSuccess) {
            return loginInfoResponse
        }

        val loginInfo = loginInfoResponse.data ?: return Response.ofFail("token is invalid")
        return if (loginInfoForToken.signature == loginInfo.signature) {
            Response.ofSuccess(loginInfo)
        } else {
            Response.ofFail("token signature is invalid")
        }
    }

    fun loginCheckWithHeader(request: HttpServletRequest): Response<LoginInfo> =
        loginCheck(request.getHeader(tokenKey))

    /**
     * Cookie 登录校验失败时主动清理浏览器里的脏 token，避免前端持续携带无效会话。
     */
    fun loginCheckWithCookie(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Response<LoginInfo> {
        ensureInitialized()
        val token = CookieTool.getValue(request, tokenKey)
        val result = loginCheck(token)
        if (!result.isSuccess) {
            CookieTool.remove(request, response, tokenKey)
        }
        return result
    }

    fun loginCheckWithAttr(request: HttpServletRequest): Response<LoginInfo> {
        val loginInfo = request.getAttribute(AuthConst.TASK_PILOT_LOGIN_USER) as? LoginInfo
        return if (loginInfo != null) Response.ofSuccess(loginInfo) else Response.ofFail("not login.")
    }

    fun hasRole(loginInfo: LoginInfo, role: String?): Response<String> {
        if (StringTool.isBlank(role)) {
            return Response.ofSuccess()
        }
        if (CollectionTool.isEmpty(loginInfo.roleList)) {
            return Response.ofFail("roleList is null.")
        }
        return if (loginInfo.roleList!!.contains(role)) Response.ofSuccess() else Response.ofFail("has no role.")
    }

    fun hasPermission(loginInfo: LoginInfo, permission: String?): Response<String> {
        if (StringTool.isBlank(permission)) {
            return Response.ofSuccess()
        }
        if (CollectionTool.isEmpty(loginInfo.permissionList)) {
            return Response.ofFail("permissionList is null.")
        }
        return if (loginInfo.permissionList!!.contains(permission)) {
            Response.ofSuccess()
        } else {
            Response.ofFail("has no permission.")
        }
    }
}
