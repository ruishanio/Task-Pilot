package com.ruishanio.taskpilot.tool.pipeline

/**
 * 流水线处理链节点。
 * 继续只做一层轻量包装，保持 `Pipeline` 内部节点结构与调用顺序不变。
 */
class PipelineChain(private val pipelineHandler: PipelineHandler) {
    fun handle(pipelineContext: PipelineContext) {
        pipelineHandler.handle(pipelineContext)
    }
}
