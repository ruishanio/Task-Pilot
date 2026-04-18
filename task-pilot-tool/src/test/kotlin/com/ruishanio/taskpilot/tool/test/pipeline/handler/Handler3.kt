package com.ruishanio.taskpilot.tool.test.pipeline.handler

import com.ruishanio.taskpilot.tool.pipeline.PipelineContext
import com.ruishanio.taskpilot.tool.pipeline.PipelineHandler
import com.ruishanio.taskpilot.tool.test.pipeline.PipelineTest
import org.slf4j.LoggerFactory

/**
 * Handler3 记录末尾处理器执行。
 */
class Handler3 : PipelineHandler() {
    override fun handle(pipelineContext: PipelineContext) {
        val request = pipelineContext.request as PipelineTest.DemoRequest
        logger.info("Handler3 run: {}", request.arg1)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Handler3::class.java)
    }
}
