package com.ruishanio.taskpilot.admin.auth.helper

import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import com.ruishanio.taskpilot.admin.auth.exception.TaskPilotAuthException
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.auth.model.LoginTokenPayload
import com.ruishanio.taskpilot.tool.auth.JwtTool
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.http.http.enums.Header
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.servlet.http.HttpServletRequest

/**
 * 管理端本地认证辅助对象。
 *
 * 统一管理 JWT 登录签发、请求头解析与 request 里的已解析登录态，避免控制器重复处理认证细节。
 */
object TaskPilotAuthHelper {
    private lateinit var jwtTool: JwtTool
    private var tokenTimeout: Long = AuthConst.EXPIRE_TIME_FOR_10_YEAR

    fun init(jwtSecret: String?, tokenTimeout: Long) {
        if (StringTool.isBlank(jwtSecret)) {
            throw TaskPilotAuthException("task-pilot jwt secret is blank.")
        }
        this.jwtTool = JwtTool(jwtSecret!!.trim())
        this.tokenTimeout = if (tokenTimeout <= 0) AuthConst.EXPIRE_TIME_FOR_10_YEAR else tokenTimeout
    }

    /**
     * 所有对外能力都依赖初始化后的 JWT 工具，未初始化直接报错，避免静默放大认证问题。
     */
    private fun ensureInitialized() {
        if (!::jwtTool.isInitialized) {
            throw TaskPilotAuthException("task-pilot auth helper not initialized.")
        }
    }

    fun login(loginInfo: LoginInfo?): Response<LoginTokenPayload> {
        ensureInitialized()
        val currentLoginInfo = loginInfo ?: return Response.ofFail("loginInfo is null")
        if (StringTool.isBlank(currentLoginInfo.userId) || StringTool.isBlank(currentLoginInfo.userName)) {
            return Response.ofFail("loginInfo is invalid")
        }

        currentLoginInfo.expireTime = System.currentTimeMillis() + tokenTimeout
        val claims = hashMapOf(
            CLAIM_USER_ID to currentLoginInfo.userId!!,
            CLAIM_USER_NAME to currentLoginInfo.userName!!,
            CLAIM_ROLE_LIST to (currentLoginInfo.roleList ?: emptyList()),
            CLAIM_EXTRA_INFO to (currentLoginInfo.extraInfo ?: emptyMap())
        )
        return try {
            val accessToken = jwtTool.createToken(currentLoginInfo.userId!!, claims, tokenTimeout)
            Response.ofSuccess(
                LoginTokenPayload(
                    accessToken = accessToken,
                    expiresAt = currentLoginInfo.expireTime
                )
            )
        } catch (ex: Exception) {
            throw TaskPilotAuthException("create jwt token fail", ex)
        }
    }

    fun logout(): Response<String> = Response.ofSuccess()

    fun loginCheck(token: String?): Response<LoginInfo> {
        ensureInitialized()
        val rawToken = resolveBearerToken(token)
        if (rawToken == null || !jwtTool.validateToken(rawToken)) {
            return Response.ofFail("token is invalid")
        }

        return try {
            val userId = resolveStringClaim(jwtTool.getClaim(rawToken, CLAIM_USER_ID))
            val userName = resolveStringClaim(jwtTool.getClaim(rawToken, CLAIM_USER_NAME))
            if (StringTool.isBlank(userId) || StringTool.isBlank(userName)) {
                Response.ofFail("token is invalid")
            } else {
                Response.ofSuccess(
                    LoginInfo(
                        userId = userId,
                        userName = userName,
                        roleList = resolveRoleList(jwtTool.getClaim(rawToken, CLAIM_ROLE_LIST)),
                        extraInfo = resolveExtraInfo(jwtTool.getClaim(rawToken, CLAIM_EXTRA_INFO)),
                        expireTime = jwtTool.getExpirationTime(rawToken)?.time ?: 0
                    )
                )
            }
        } catch (_: Exception) {
            Response.ofFail("token is invalid")
        }
    }

    fun loginCheckWithHeader(request: HttpServletRequest): Response<LoginInfo> =
        loginCheck(request.getHeader(Header.AUTHORIZATION.value))

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

    /**
     * Header 必须严格符合 `Bearer <token>`，避免兼容过多非标准写法后把鉴权边界做模糊。
     */
    private fun resolveBearerToken(authorizationHeader: String?): String? {
        if (StringTool.isBlank(authorizationHeader)) {
            return null
        }
        val normalizedHeader = authorizationHeader!!.trim()
        if (!normalizedHeader.startsWith(AuthConst.BEARER_TOKEN_PREFIX, ignoreCase = true)) {
            return null
        }
        val rawToken = normalizedHeader.substring(AuthConst.BEARER_TOKEN_PREFIX.length).trim()
        return if (StringTool.isBlank(rawToken)) null else rawToken
    }

    /**
     * 角色列表写入 JWT 时保持 JSON 数组，回读时只保留非空字符串，避免脏 claim 混进授权判断。
     */
    private fun resolveRoleList(rawRoleList: Any?): List<String>? {
        val roleList = (rawRoleList as? List<*>)
            ?.mapNotNull { role ->
                role?.toString()?.trim()?.takeIf(StringTool::isNotBlank)
            }
            ?: emptyList()
        return roleList.ifEmpty { null }
    }

    /**
     * 标量 claim 统一按字符串读取，避免不同 JSON 解析器把同一字段还原成非字符串实现细节。
     */
    private fun resolveStringClaim(rawValue: Any?): String? =
        rawValue?.toString()?.trim()?.takeIf(StringTool::isNotBlank)

    /**
     * 扩展信息当前只依赖 `executorIds`，但这里仍按通用字符串 Map 回填，便于后续扩展 claim。
     */
    private fun resolveExtraInfo(rawExtraInfo: Any?): Map<String, String>? {
        val extraInfo = (rawExtraInfo as? Map<*, *>)
            ?.mapNotNull { entry ->
                val key = entry.key?.toString()?.trim()
                if (StringTool.isBlank(key)) {
                    null
                } else {
                    key!! to (entry.value?.toString() ?: "")
                }
            }
            ?.toMap()
            ?: emptyMap()
        return extraInfo.ifEmpty { null }
    }

    private const val CLAIM_USER_ID = "userId"
    private const val CLAIM_USER_NAME = "userName"
    private const val CLAIM_ROLE_LIST = "roleList"
    private const val CLAIM_EXTRA_INFO = "extraInfo"
}
