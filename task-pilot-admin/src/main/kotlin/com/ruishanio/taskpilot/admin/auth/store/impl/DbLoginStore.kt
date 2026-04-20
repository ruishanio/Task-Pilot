package com.ruishanio.taskpilot.admin.auth.store.impl

import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.admin.auth.store.LoginStore
import com.ruishanio.taskpilot.admin.constant.Consts
import com.ruishanio.taskpilot.admin.mapper.UserMapper
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 基于 `"user".token` 的本地登录态存储。
 *
 * 管理端只需要单系统 Cookie 会话，这里直接复用现有用户表，避免再维护独立认证存储。
 */
@Component
class DbLoginStore : LoginStore {
    @Resource
    private lateinit var userMapper: UserMapper

    override fun set(loginInfo: LoginInfo?): Response<String> = persistToken(loginInfo)

    override fun update(loginInfo: LoginInfo?): Response<String> = persistToken(loginInfo)

    override fun remove(userId: String?): Response<String> {
        val normalizedUserId = userId ?: return Response.ofFail("userId is blank")
        val ret = userMapper.updateToken(normalizedUserId.toInt(), "")
        return if (ret > 0) Response.ofSuccess() else Response.ofFail("token remove fail")
    }

    /**
     * 从数据库恢复当前用户的角色与任务组权限，保证控制器和服务层仍拿到完整登录上下文。
     */
    override fun get(userId: String?): Response<LoginInfo> {
        val normalizedUserId = userId ?: return Response.ofFail("userId is blank")
        val user = userMapper.loadById(normalizedUserId.toInt())
        if (user == null) {
            return Response.ofFail("userId invalid.")
        }

        val roleList = if (user.role == 1) listOf(Consts.ADMIN_ROLE) else null
        val extraInfo = mapOf("executorIds" to (user.permission ?: ""))
        val loginInfo = LoginInfo(
            userId = normalizedUserId,
            userName = user.username,
            roleList = roleList,
            extraInfo = extraInfo,
            signature = user.token
        )
        return Response.ofSuccess(loginInfo)
    }

    /**
     * 会话签名统一回写到用户表，保证后续登录会覆盖旧会话。
     */
    private fun persistToken(loginInfo: LoginInfo?): Response<String> {
        val currentLoginInfo = loginInfo ?: return Response.ofFail("loginInfo is null")
        val userId = currentLoginInfo.userId ?: return Response.ofFail("userId is blank")
        val tokenSign = currentLoginInfo.signature ?: return Response.ofFail("signature is blank")
        val ret = userMapper.updateToken(userId.toInt(), tokenSign)
        return if (ret > 0) Response.ofSuccess() else Response.ofFail("token set fail")
    }
}
