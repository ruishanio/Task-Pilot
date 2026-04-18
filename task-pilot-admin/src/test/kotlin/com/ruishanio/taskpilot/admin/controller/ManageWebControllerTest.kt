package com.ruishanio.taskpilot.admin.controller

import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 验证 `/web/` 前缀页面入口已切换为 history 路由，并统一 forward 到前端 SPA 入口。
 */
class ManageWebControllerTest : AbstractSpringMvcTest() {
    private lateinit var cookie: Cookie

    @BeforeEach
    @Throws(Exception::class)
    fun login() {
        val ret =
            mockMvc
                .perform(
                    post("/api/manage/auth/doLogin")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456")
                ).andReturn()
        cookie = ret.response.getCookie(AuthConst.TASK_PILOT_LOGIN_TOKEN)!!
    }

    @Test
    @Throws(Exception::class)
    fun loginPageForwardsToSpaWhenAnonymous() {
        mockMvc.perform(get("/web/login"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/web/index.html"))
    }

    @Test
    @Throws(Exception::class)
    fun rootRedirectsToDashboardWhenLoggedIn() {
        mockMvc.perform(get("/").cookie(cookie))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/web/dashboard"))
    }

    @Test
    @Throws(Exception::class)
    fun dashboardForwardsToSpaWhenLoggedIn() {
        mockMvc.perform(get("/web/dashboard").cookie(cookie))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/web/index.html"))
    }
}
