package com.ruishanio.taskpilot.tool.test.response

import com.ruishanio.taskpilot.tool.response.Response
import com.ruishanio.taskpilot.tool.response.ResponseCode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Response 工厂方法验证。
 */
class ResponseTest {
    @Test
    fun testResponse() {
        Assertions.assertEquals(Response.ofSuccess<Any>().code, ResponseCode.SUCCESS.code)
        Assertions.assertEquals(Response.ofSuccess("hello").code, ResponseCode.SUCCESS.code)
        Assertions.assertEquals(Response.ofSuccess("hello").data, "hello")

        Assertions.assertEquals(Response.ofFail<Any>().code, ResponseCode.FAILURE.code)
        Assertions.assertEquals(Response.ofFail<Any>("hello").msg, "hello")

        val result3: Response<String>? = Response.of(200, "message", "hello")
        Assertions.assertEquals(result3?.code, 200)
        Assertions.assertEquals(result3?.msg, "message")
        Assertions.assertEquals(result3?.data, "hello")
    }
}
