package com.ruishanio.taskpilot.tool.test.pipeline

import com.ruishanio.taskpilot.tool.pipeline.Pipeline
import com.ruishanio.taskpilot.tool.pipeline.PipelineHandler
import com.ruishanio.taskpilot.tool.pipeline.PipelineStatus
import com.ruishanio.taskpilot.tool.response.Response
import com.ruishanio.taskpilot.tool.response.ResponseCode
import com.ruishanio.taskpilot.tool.test.pipeline.handler.Handler1
import com.ruishanio.taskpilot.tool.test.pipeline.handler.Handler2
import com.ruishanio.taskpilot.tool.test.pipeline.handler.Handler3
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * Pipeline 单条流水线执行验证。
 */
class PipelineTest {
    @Test
    fun testPipeline() {
        val handler1: PipelineHandler = Handler1()
        val handler2: PipelineHandler = Handler2()
        val handler3: PipelineHandler = Handler3()

        val p1 =
            Pipeline()
                .name("p1")
                .status(PipelineStatus.RUNTIME.status)
                .addLasts(handler1, handler2, handler3)

        val request = DemoRequest("abc", 100)
        val response: Response<Any>? = p1.process(request)

        logger.info("response: {}", response)
        Assertions.assertEquals(response?.code, ResponseCode.SUCCESS.code)
    }

    /**
     * 流水线测试请求对象。
     */
    data class DemoRequest(
        var arg1: String? = null,
        var arg2: Int = 0,
    )

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTest::class.java)
    }
}
