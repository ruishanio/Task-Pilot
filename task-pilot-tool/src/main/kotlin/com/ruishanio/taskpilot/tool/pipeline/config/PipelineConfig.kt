package com.ruishanio.taskpilot.tool.pipeline.config

/**
 * 流水线配置模型。
 * 字段保持可空，兼容旧配置装配过程中可能出现的半初始化对象。
 */
class PipelineConfig() {
    var name: String? = null
    var handlerList: List<String>? = null

    constructor(name: String?, handlerList: List<String>?) : this() {
        this.name = name
        this.handlerList = handlerList
    }

    override fun toString(): String = "PipelineConfig{name='$name', handlerList=$handlerList}"
}
