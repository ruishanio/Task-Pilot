package com.ruishanio.taskpilot.tool.pipeline

import com.ruishanio.taskpilot.tool.response.Response
import java.util.ArrayList
import java.util.concurrent.ConcurrentMap
import org.slf4j.LoggerFactory

/**
 * 流水线定义。
 * 保留链式构建与顺序执行模型，不在 Kotlin 迁移阶段改变异常兜底和结束日志级别等历史行为。
 */
class Pipeline {
    var name: String? = null
    var status: String? = PipelineStatus.RUNTIME.status
    val handlerList: MutableList<PipelineChain> = ArrayList()

    fun name(name: String?): Pipeline {
        this.name = name
        return this
    }

    fun status(status: String?): Pipeline {
        this.status = status
        return this
    }

    override fun toString(): String =
        "Pipeline{name='$name', status='$status', handlerList=$handlerList}"

    fun addLast(pipelineHandler: PipelineHandler): Pipeline {
        val pipelineChain = PipelineChain(pipelineHandler)
        handlerList.add(pipelineChain)
        return this
    }

    fun addLasts(vararg pipelineHandlers: PipelineHandler): Pipeline {
        for (pipelineHandler in pipelineHandlers) {
            addLast(pipelineHandler)
        }
        return this
    }

    fun process(request: Any?): Response<Any>? = process(request, null)

    /**
     * 处理链中的异常统一转成失败响应并中断后续执行，保持原来的“吞异常、写日志、返回失败”路径。
     */
    fun process(request: Any?, contextMap: ConcurrentMap<String, Any>?): Response<Any>? {
        logger.debug("流水线开始执行，name={}, status={}", name, status)

        val pipelineContext = PipelineContext(request)
        if (contextMap != null) {
            pipelineContext.contextMap = contextMap
        }

        for (handlerChain in handlerList) {
            if (pipelineContext.isBreak) {
                break
            }
            try {
                handlerChain.handle(pipelineContext)
            } catch (e: Exception) {
                logger.error("流水线执行异常，name={}, pipelineContext={}", name, pipelineContext, e)
                pipelineContext.breakToFail(Response.ofFail(e.message))
            }
        }

        if (pipelineContext.response == null) {
            pipelineContext.breakToFail(Response.ofFail("pipeline response not found."))
        }

        logger.error("流水线执行结束，name={}, pipelineContext={}", name, pipelineContext)
        return pipelineContext.response
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Pipeline::class.java)
    }
}
