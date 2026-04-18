package com.ruishanio.taskpilot.tool.test.pipeline.handler

import com.ruishanio.taskpilot.tool.pipeline.PipelineContext
import com.ruishanio.taskpilot.tool.pipeline.PipelineHandler
import com.ruishanio.taskpilot.tool.test.pipeline.PipelineTest
import org.slf4j.LoggerFactory

/**
 * Handler2 用于验证处理中断分支。
 */
class Handler2 : PipelineHandler() {
    override fun handle(pipelineContext: PipelineContext) {
        val request = pipelineContext.request as PipelineTest.DemoRequest

        if (request.arg1 == "jack") {
            logger.info("Handler2 error: {}", request.arg1)
            pipelineContext.breakToFail()
            return
        }

        logger.info("Handler2 run: {}", request.arg1)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Handler2::class.java)
    }
}
