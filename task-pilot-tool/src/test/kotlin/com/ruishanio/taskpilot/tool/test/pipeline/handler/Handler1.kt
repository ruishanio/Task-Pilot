package com.ruishanio.taskpilot.tool.test.pipeline.handler

import com.ruishanio.taskpilot.tool.pipeline.PipelineContext
import com.ruishanio.taskpilot.tool.pipeline.PipelineHandler
import com.ruishanio.taskpilot.tool.test.pipeline.PipelineTest
import org.slf4j.LoggerFactory

/**
 * Handler1 仅记录请求参数，验证流水线顺序执行。
 */
class Handler1 : PipelineHandler() {
    override fun handle(pipelineContext: PipelineContext) {
        val request = pipelineContext.request as PipelineTest.DemoRequest
        logger.info("Handler1 run: {}", request.arg1)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Handler1::class.java)
    }
}
