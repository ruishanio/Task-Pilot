package com.ruishanio.taskpilot.admin.controller

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 验证本地认证拦截器对页面与 JSON 接口的分流行为。
 */
class AuthWebMvcTest : AbstractSpringMvcTest() {
    @Test
    @Throws(Exception::class)
    fun pageRequestRedirectsToLoginWhenAnonymous() {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/web/login"))
    }

    @Test
    @Throws(Exception::class)
    fun bootstrapReturns401WhenAnonymous() {
        mockMvc.perform(get("/api/manage/system/bootstrap"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("\"code\":401")))
    }
}
