package com.ruishanio.taskpilot.tool.pipeline.config.impl

import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.pipeline.config.PipelineConfig
import com.ruishanio.taskpilot.tool.pipeline.config.PipelineConfigLoader
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

/**
 * 本地流水线配置加载器。
 * 保留基于内存 Map 的注册和查询方式，避免迁移时引入额外配置源差异。
 */
class LocalPipelineConfigLoader : PipelineConfigLoader {
    private val pipelineConfigMap: MutableMap<String, PipelineConfig> = ConcurrentHashMap()

    fun registry(pipelineConfig: PipelineConfig): Boolean {
        if (StringTool.isBlank(pipelineConfig.name) || CollectionTool.isEmpty(pipelineConfig.handlerList)) {
            return false
        }

        pipelineConfigMap.putIfAbsent(pipelineConfig.name!!, pipelineConfig)
        return true
    }

    override fun load(name: String?): PipelineConfig? = pipelineConfigMap[name]

    override fun loadAll(): List<PipelineConfig> = ArrayList(pipelineConfigMap.values)
}
