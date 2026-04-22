package com.ruishanio.taskpilot.admin.controller

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 验证 `/web/` 前缀页面入口已切换为 history 路由，并统一 forward 到前端 SPA 入口。
 */
class ManageWebControllerTest : AbstractSpringMvcTest() {
    @Test
    @Throws(Exception::class)
    fun loginPageForwardsToSpaWhenAnonymous() {
        mockMvc.perform(get("/web/login"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/web/index.html"))
    }

    @Test
    @Throws(Exception::class)
    fun rootRedirectsToDashboardWhenAnonymous() {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/web/dashboard"))
    }

    @Test
    @Throws(Exception::class)
    fun dashboardForwardsToSpaWhenAnonymous() {
        mockMvc.perform(get("/web/dashboard"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/web/index.html"))
    }

    @Test
    @Throws(Exception::class)
    fun nestedFrontendRouteForwardsToSpaWhenAnonymous() {
        mockMvc.perform(get("/web/task_log/detail"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/web/index.html"))
    }

    @Test
    @Throws(Exception::class)
    fun futureFrontendRouteForwardsToSpaWhenAnonymous() {
        mockMvc.perform(get("/web/future/module/route"))
            .andExpect(status().isOk)
            .andExpect(forwardedUrl("/web/index.html"))
    }
}
