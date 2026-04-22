package com.ruishanio.taskpilot.admin.auth.model

import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import java.io.Serializable

/**
 * 登录成功后返回给前端的 JWT 载荷。
 */
data class LoginTokenPayload(
    /**
     * 访问令牌，由前端按 Bearer 方案持久化并回传。
     */
    var accessToken: String? = null,
    /**
     * 当前仅支持 Bearer，显式返回可减少前后端各自硬编码。
     */
    var tokenType: String = AuthConst.BEARER_TOKEN_TYPE,
    /**
     * 过期时间，毫秒时间戳。
     */
    var expiresAt: Long = 0
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
