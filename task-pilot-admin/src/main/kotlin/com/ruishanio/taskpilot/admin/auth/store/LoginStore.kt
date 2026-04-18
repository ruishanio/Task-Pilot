package com.ruishanio.taskpilot.admin.auth.store

import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.tool.response.Response

/**
 * 管理端登录态存储接口。
 */
interface LoginStore {
    fun start() {
        // 默认无需额外初始化。
    }

    fun stop() {
        // 默认无需额外销毁资源。
    }

    fun set(loginInfo: LoginInfo?): Response<String>

    fun update(loginInfo: LoginInfo?): Response<String>

    fun remove(userId: String?): Response<String>

    fun get(userId: String?): Response<LoginInfo>
}
