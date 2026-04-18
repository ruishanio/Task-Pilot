package com.ruishanio.taskpilot.tool.test.pipeline

import com.ruishanio.taskpilot.tool.pipeline.Pipeline
import com.ruishanio.taskpilot.tool.pipeline.PipelineExecutor
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
 * PipelineExecutor 注册与执行验证。
 */
class PipelineExecutorTest {
    @Test
    fun testPipelineExecutor() {
        val handler1: PipelineHandler = Handler1()
        val handler2: PipelineHandler = Handler2()
        val handler3: PipelineHandler = Handler3()

        val p1 =
            Pipeline()
                .name("p1")
                .status(PipelineStatus.RUNTIME.status)
                .addLasts(handler1, handler2, handler3)

        val p2 =
            Pipeline()
                .name("p2")
                .status(PipelineStatus.RUNTIME.status)
                .addLasts(handler2, handler1, handler3)

        val executor = PipelineExecutor()
        executor.registry(p1)
        executor.registry(p2)

        val request1 = PipelineTest.DemoRequest("aaa", 100)
        val request2 = PipelineTest.DemoRequest("bbb", 100)

        val response1: Response<Any>? = p1.process(request1)
        logger.info("response1: {}", response1)
        Assertions.assertEquals(response1?.code, ResponseCode.SUCCESS.code)

        val response2: Response<Any>? = p2.process(request2)
        logger.info("response2: {}", response2)
        Assertions.assertEquals(response2?.code, ResponseCode.SUCCESS.code)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineExecutorTest::class.java)
    }
}
