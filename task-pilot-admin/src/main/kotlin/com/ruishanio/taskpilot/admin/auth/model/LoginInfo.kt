package com.ruishanio.taskpilot.admin.auth.model

import java.io.Serializable

/**
 * 管理端登录态信息。
 */
data class LoginInfo(
    /**
     * 用户唯一标识。
     */
    var userId: String? = null,
    /**
     * 用户名。
     */
    var userName: String? = null,
    /**
     * 真实姓名。
     */
    var realName: String? = null,
    /**
     * 扩展信息。
     */
    var extraInfo: Map<String, String>? = null,
    /**
     * 角色列表。
     */
    var roleList: List<String>? = null,
    /**
     * 权限列表。
     */
    var permissionList: List<String>? = null,
    /**
     * 过期时间，毫秒时间戳。
     */
    var expireTime: Long = 0,
    /**
     * 历史签名字段暂时保留给旧同步链路复用，管理端 JWT 鉴权主链路已不再依赖它。
     */
    var signature: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
