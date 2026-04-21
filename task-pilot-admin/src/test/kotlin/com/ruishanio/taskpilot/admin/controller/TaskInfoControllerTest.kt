package com.ruishanio.taskpilot.admin.controller

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
import tools.jackson.databind.json.JsonMapper

/**
 * 覆盖任务管理控制器的基础登录态访问与调度时间试算能力。
 */
class TaskInfoControllerTest : AbstractSpringMvcTest() {
    private lateinit var cookie: Cookie
    // 测试侧也显式走 Jackson 3，避免被 classpath 上并存的 Jackson 2 旧包误导。
    private val jsonMapper = JsonMapper.builder().build()

    @BeforeEach
    @Throws(Exception::class)
    fun login() {
        val ret =
            mockMvc
                .perform(
                    post("/api/manage/auth/login")
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
                    post("/api/manage/task_info/page")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(parameters)
                        .cookie(cookie)
                ).andReturn()

        val body = jsonMapper.readTree(ret.response.contentAsString)
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
                    post("/api/manage/task_info/next_trigger_time")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("scheduleType", "FIX_RATE")
                        .param("scheduleConf", "5")
                        .cookie(cookie)
                ).andReturn()

        val body = jsonMapper.readTree(ret.response.contentAsString)
        assertEquals(200, body["code"].asInt())
        assertTrue(body["data"].isArray)
        assertEquals(5, body["data"].size())
    }
}
