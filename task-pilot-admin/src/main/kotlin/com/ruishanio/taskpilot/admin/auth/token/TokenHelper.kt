package com.ruishanio.taskpilot.admin.auth.token

import com.ruishanio.taskpilot.admin.auth.model.LoginInfo
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.crypto.Base64Tool
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.response.Response

/**
 * 本地认证 token 工具。
 *
 * token 里只保留 `userId + signature`，避免把完整登录态直接暴露给客户端。
 */
object TokenHelper {
    /**
     * 根据登录态生成轻量 token。
     */
    fun generateToken(loginInfo: LoginInfo?): Response<String> {
        if (loginInfo == null || StringTool.isBlank(loginInfo.userId) || StringTool.isBlank(loginInfo.signature)) {
            return Response.ofFail("generateToken fail, invalid loginInfo.")
        }

        val loginInfoForToken = LoginInfo(
            userId = loginInfo.userId,
            signature = loginInfo.signature
        )
        val json = GsonTool.toJson(loginInfoForToken)
        val token = Base64Tool.encodeUrlSafe(json)
        return Response.ofSuccess(token)
    }

    /**
     * 从 token 解析出轻量登录态。
     */
    fun parseToken(token: String?): LoginInfo? =
        try {
            if (StringTool.isBlank(token)) {
                null
            } else {
                val json = Base64Tool.decodeUrlSafe(token!!)
                GsonTool.fromJson(json, LoginInfo::class.java)
            }
        } catch (_: Exception) {
            null
        }
}
