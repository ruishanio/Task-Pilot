package com.ruishanio.taskpilot.admin.controller

import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

/**
 * 验证任务分页接口在登录态下可访问。
 */
class JobInfoControllerTest : AbstractSpringMvcTest() {
    private lateinit var cookie: Cookie

    @BeforeEach
    @Throws(Exception::class)
    fun login() {
        val ret =
            mockMvc
                .perform(
                    post("/auth/doLogin")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("userName", "admin")
                        .param("password", "123456")
                ).andReturn()
        cookie = ret.response.getCookie(AuthConst.TASK_PILOT_LOGIN_TOKEN)!!
    }

    @Test
    @Throws(Exception::class)
    fun testAdd() {
        val parameters: MultiValueMap<String, String> = LinkedMultiValueMap()
        parameters.add("jobGroup", "1")
        parameters.add("triggerStatus", "-1")

        val ret: MvcResult =
            mockMvc
                .perform(
                    post("/jobinfo/pageList")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(parameters)
                        .cookie(cookie)
                ).andReturn()

        logger.info(ret.response.contentAsString)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobInfoControllerTest::class.java)
    }
}
