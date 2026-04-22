package com.ruishanio.taskpilot.admin.auth.helper

import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import com.ruishanio.taskpilot.admin.auth.exception.TaskPilotAuthException
import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.auth.model.LoginTokenPayload
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import java.time.Instant

/**
 * 管理端本地认证辅助对象。
 *
 * 统一管理 JWT 登录签发，以及 request / SecurityContext 里的业务登录态恢复逻辑，避免控制器重复处理认证细节。
 */
object TaskPilotAuthHelper {
    /**
     * 官方 JwtEncoder 在启动时初始化一次，后续登录签发都复用同一个实例。
     */
    private lateinit var jwtEncoder: JwtEncoder
    /**
     * Token 有效期继续沿用配置项毫秒值，避免前后端各自推导过期时间。
     */
    private var tokenTimeout: Long = AuthConst.EXPIRE_TIME_FOR_10_YEAR

    /**
     * 管理端启动时初始化官方 JWT 编码器与过期时间，避免登录接口自行拼装签名逻辑。
     */
    fun init(jwtEncoder: JwtEncoder, tokenTimeout: Long) {
        this.jwtEncoder = jwtEncoder
        this.tokenTimeout = if (tokenTimeout <= 0) AuthConst.EXPIRE_TIME_FOR_10_YEAR else tokenTimeout
    }

    /**
     * 所有对外能力都依赖初始化后的 JwtEncoder，未初始化直接报错，避免静默放大认证问题。
     */
    private fun ensureInitialized() {
        if (!::jwtEncoder.isInitialized) {
            throw TaskPilotAuthException("task-pilot auth helper not initialized.")
        }
    }

    /**
     * 登录成功后把最小必需登录态写进 JWT，既保证前端能无状态续用，又避免把完整用户对象暴露给客户端。
     */
    fun login(loginInfo: LoginInfo?): Response<LoginTokenPayload> {
        ensureInitialized()
        val currentLoginInfo = loginInfo ?: return Response.ofFail("loginInfo is null")
        if (StringTool.isBlank(currentLoginInfo.userId) || StringTool.isBlank(currentLoginInfo.userName)) {
            return Response.ofFail("loginInfo is invalid")
        }

        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plusMillis(tokenTimeout)
        currentLoginInfo.expireTime = expiresAt.toEpochMilli()
        val claims = JwtClaimsSet.builder()
            .subject(currentLoginInfo.userId!!)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim(CLAIM_USER_ID, currentLoginInfo.userId!!)
            .claim(CLAIM_USER_NAME, currentLoginInfo.userName!!)
            .claim(CLAIM_ROLE_LIST, currentLoginInfo.roleList ?: emptyList<String>())
            .claim(CLAIM_EXTRA_INFO, currentLoginInfo.extraInfo ?: emptyMap<String, String>())
            .build()
        return try {
            val accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    claims
                )
            ).tokenValue
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

    /**
     * 官方 JwtDecoder 会先完成验签与时间校验，这里只负责把 claims 还原成业务层沿用的 `LoginInfo`。
     */
    fun fromJwt(jwt: Jwt?): LoginInfo? {
        val currentJwt = jwt ?: return null
        val claims = currentJwt.claims
        val userId = resolveStringClaim(claims[CLAIM_USER_ID] ?: currentJwt.subject)
        val userName = resolveStringClaim(claims[CLAIM_USER_NAME])
        if (StringTool.isBlank(userId) || StringTool.isBlank(userName)) {
            return null
        }

        return LoginInfo(
            userId = userId,
            userName = userName,
            roleList = resolveRoleList(claims[CLAIM_ROLE_LIST]),
            extraInfo = resolveExtraInfo(claims[CLAIM_EXTRA_INFO]),
            expireTime = currentJwt.expiresAt?.toEpochMilli() ?: 0
        )
    }

    /**
     * Security 过滤器会优先把登录态挂到 request，若当前调用链没有显式透传 request attribute，则回退读取 SecurityContext。
     */
    fun loginCheckWithAttr(request: HttpServletRequest): Response<LoginInfo> {
        val loginInfo = request.getAttribute(AuthConst.TASK_PILOT_LOGIN_USER) as? LoginInfo
            ?: resolveLoginInfoFromSecurityContext()
        return if (loginInfo != null) Response.ofSuccess(loginInfo) else Response.ofFail("not login.")
    }

    /**
     * 角色校验只处理注解显式声明的角色要求，空角色直接放行，避免普通接口被额外角色约束误伤。
     */
    fun hasRole(loginInfo: LoginInfo, role: String?): Response<String> {
        if (StringTool.isBlank(role)) {
            return Response.ofSuccess()
        }
        if (CollectionTool.isEmpty(loginInfo.roleList)) {
            return Response.ofFail("roleList is null.")
        }
        return if (loginInfo.roleList!!.contains(role)) Response.ofSuccess() else Response.ofFail("has no role.")
    }

    /**
     * 权限校验保留原语义，只有接口声明了具体权限值时才校验，方便后续继续扩展细粒度授权。
     */
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

    /**
     * 业务层仍大量依赖 `LoginInfo`，这里把 Spring Security 的 principal 还原成旧模型，降低迁移改动面。
     */
    private fun resolveLoginInfoFromSecurityContext(): LoginInfo? =
        when (val principal = SecurityContextHolder.getContext().authentication?.principal) {
            is LoginInfo -> principal
            is Jwt -> fromJwt(principal)
            else -> null
        }

    private const val CLAIM_USER_ID = "userId"
    private const val CLAIM_USER_NAME = "userName"
    private const val CLAIM_ROLE_LIST = "roleList"
    private const val CLAIM_EXTRA_INFO = "extraInfo"
}
