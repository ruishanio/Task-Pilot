package com.ruishanio.taskpilot.admin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ruishanio.taskpilot.admin.auth.constant.AuthConst
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

/**
 * 覆盖任务管理控制器的基础登录态访问与调度时间试算能力。
 */
class JobInfoControllerTest : AbstractSpringMvcTest() {
    private lateinit var cookie: Cookie
    private val objectMapper = ObjectMapper()

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
    fun pageListShouldReturnSuccessUnderLoginContext() {
        val parameters: MultiValueMap<String, String> = LinkedMultiValueMap()
        parameters.add("executorId", "1")
        parameters.add("triggerStatus", "-1")

        val ret: MvcResult =
            mockMvc
                .perform(
                    post("/api/manage/task_info/pageList")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(parameters)
                        .cookie(cookie)
                ).andReturn()

        val body = objectMapper.readTree(ret.response.contentAsString)
        assertEquals(200, body["code"].asInt())
        assertTrue(body["data"].has("data"))
        assertTrue(body["data"].has("total"))
    }

    @Test
    @Throws(Exception::class)
    fun nextTriggerTimeShouldAcceptEnumParameterAndReturnFiveSchedulePoints() {
        val ret =
            mockMvc
                .perform(
                    post("/api/manage/task_info/nextTriggerTime")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("scheduleType", "FIX_RATE")
                        .param("scheduleConf", "5")
                        .cookie(cookie)
                ).andReturn()

        val body = objectMapper.readTree(ret.response.contentAsString)
        assertEquals(200, body["code"].asInt())
        assertTrue(body["data"].isArray)
        assertEquals(5, body["data"].size())
    }
}
