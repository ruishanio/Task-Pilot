package com.ruishanio.taskpilot.admin.controller

import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import com.ruishanio.taskpilot.tool.auth.JwtTool
import com.ruishanio.taskpilot.tool.http.http.enums.Header
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 验证 Bearer 鉴权拦截器对页面入口放行与 JSON 接口 401 行为。
 */
class AuthWebMvcTest : AbstractSpringMvcTest() {
    @Value("\${task-pilot.auth.jwt.secret}")
    private lateinit var jwtSecret: String

    @Test
    @Throws(Exception::class)
    fun pageRequestRedirectsToDashboardWhenAnonymous() {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/web/dashboard"))
    }

    @Test
    @Throws(Exception::class)
    fun bootstrapReturns401WhenAnonymous() {
        mockMvc.perform(get("/api/manage/system/bootstrap"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("\"code\":401")))
    }

    @Test
    @Throws(Exception::class)
    fun bootstrapReturns401WhenTokenSignatureIsInvalid() {
        val invalidToken = createBearerToken(
            "another-task-pilot-jwt-secret-key-for-invalid-signature-check"
        )

        mockMvc.perform(get("/api/manage/system/bootstrap").header(Header.AUTHORIZATION.value, invalidToken))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("\"code\":401")))
    }

    @Test
    @Throws(Exception::class)
    fun bootstrapReturns401WhenTokenExpired() {
        val expiredToken = createBearerToken(jwtSecret, -1000)

        mockMvc.perform(get("/api/manage/system/bootstrap").header(Header.AUTHORIZATION.value, expiredToken))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("\"code\":401")))
    }

    /**
     * 测试 token 只保留管理端鉴权所需最小 claim，避免把无关状态混进断言。
     */
    private fun createBearerToken(secret: String, ttlMillis: Long = 60_000): String {
        val jwtTool = JwtTool(secret)
        val token = jwtTool.createToken(
            "1",
            mapOf(
                "userId" to "1",
                "userName" to "admin",
                "roleList" to listOf("admin"),
                "extraInfo" to mapOf("executorIds" to "")
            ),
            ttlMillis
        )
        return "${AuthConst.BEARER_TOKEN_PREFIX}$token"
    }
}
