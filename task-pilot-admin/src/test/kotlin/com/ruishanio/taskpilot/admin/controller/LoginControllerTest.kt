package com.ruishanio.taskpilot.admin.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import tools.jackson.databind.json.JsonMapper

/**
 * 验证登录接口已切换为返回 Bearer JWT，而不是写入 Cookie。
 */
class LoginControllerTest : AbstractSpringMvcTest() {
    private val jsonMapper = JsonMapper.builder().build()

    @Test
    @Throws(Exception::class)
    fun loginReturnsBearerTokenPayloadWithoutCookie() {
        val ret =
            mockMvc
                .perform(
                    post("/api/manage/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456")
                        .param("ifRemember", "on")
                ).andReturn()

        val body = jsonMapper.readTree(ret.response.contentAsString)
        assertEquals(200, body["code"].asInt())
        assertEquals("Bearer", body["data"]["tokenType"].asText())
        assertNotNull(body["data"]["accessToken"].asText())
        assertTrue(body["data"]["expiresAt"].asLong() > 0)
        assertNull(ret.response.getHeader("Set-Cookie"))
    }
}
