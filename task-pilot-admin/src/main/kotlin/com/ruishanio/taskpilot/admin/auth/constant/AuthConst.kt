package com.ruishanio.taskpilot.admin.auth.constant

/**
 * 管理端本地认证常量。
 */
object AuthConst {
    /**
     * 默认登录页地址。
     */
    const val LOGIN_URL: String = "/web/login"

    /**
     * Bearer token 类型与前缀，统一收口避免前后端各自拼接。
     */
    const val BEARER_TOKEN_TYPE: String = "Bearer"
    const val BEARER_TOKEN_PREFIX: String = "$BEARER_TOKEN_TYPE "

    /**
     * 已解析登录用户在 request attribute 中的键名。
     */
    const val TASK_PILOT_LOGIN_USER: String = "task_pilot_login_user"

    /**
     * 未登录或登录失效。
     */
    const val CODE_LOGIN_FAIL: Int = 401

    /**
     * 权限不足。
     */
    const val CODE_PERMISSION_FAIL: Int = 403

    /**
     * 角色不足。
     */
    const val CODE_ROLE_FAIL: Int = 403

    /**
     * 时间常量统一使用毫秒，避免配置项误把秒当毫秒。
     */
    const val EXPIRE_TIME_1_DAY: Long = 1000L * 60 * 60 * 24
    const val EXPIRE_TIME_FOR_7_DAY: Long = EXPIRE_TIME_1_DAY * 7
    const val EXPIRE_TIME_FOR_10_YEAR: Long = EXPIRE_TIME_1_DAY * 365 * 10
}
