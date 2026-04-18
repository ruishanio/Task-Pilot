package com.ruishanio.taskpilot.tool.pipeline

import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.response.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import org.slf4j.LoggerFactory

/**
 * 流水线执行器。
 * 注册和执行入口继续保持最小封装，失败时仍返回 `null`，避免影响现有调用方的判空逻辑。
 */
open class PipelineExecutor {
    private val pipelineMap: MutableMap<String, Pipeline> = ConcurrentHashMap()

    fun registry(pipeline: Pipeline?): Boolean {
        if (pipeline == null) {
            logger.debug("PipelineExecutor 注册失败，pipeline 为空。")
            return false
        }
        if (CollectionTool.isEmpty(pipeline.handlerList)) {
            logger.debug("PipelineExecutor 注册失败，pipeline[{}] 的处理链为空。", pipeline)
            return false
        }
        pipelineMap[pipeline.name!!] = pipeline
        return true
    }

    fun execute(pipelineName: String?, request: Any?): Response<Any>? = execute(pipelineName, request, null)

    private fun execute(
        pipelineName: String?,
        request: Any?,
        contextMap: ConcurrentMap<String, Any>?
    ): Response<Any>? {
        if (StringTool.isBlank(pipelineName)) {
            logger.debug("PipelineExecutor 执行失败，pipelineName 为空。")
            return null
        }
        val pipeline = pipelineMap[pipelineName]
        if (pipeline == null) {
            logger.debug("PipelineExecutor 执行失败，未找到 pipeline[{}]。", pipelineName)
            return null
        }
        if (PipelineStatus.RUNTIME != PipelineStatus.findByStatus(pipeline.status)) {
            logger.debug("PipelineExecutor 执行失败，pipeline[{}] 当前未运行。", pipelineName)
            return null
        }

        return pipeline.process(request, contextMap)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineExecutor::class.java)
    }
}
