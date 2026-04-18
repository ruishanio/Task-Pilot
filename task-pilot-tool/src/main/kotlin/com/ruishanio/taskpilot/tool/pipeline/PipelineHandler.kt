package com.ruishanio.taskpilot.tool.pipeline

/**
 * 流水线处理器抽象。
 * 继续使用抽象类而不是函数式接口，避免影响现有 Spring Bean 或继承式处理器定义。
 */
abstract class PipelineHandler {
    abstract fun handle(pipelineContext: PipelineContext)
}
