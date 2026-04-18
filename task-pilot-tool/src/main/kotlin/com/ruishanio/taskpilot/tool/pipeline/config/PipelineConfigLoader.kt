package com.ruishanio.taskpilot.tool.pipeline.config

/**
 * 流水线配置加载器。
 * `load` 继续允许返回空值，交由调用方按“未找到配置”路径处理。
 */
interface PipelineConfigLoader {
    fun load(name: String?): PipelineConfig?

    fun loadAll(): List<PipelineConfig>
}
