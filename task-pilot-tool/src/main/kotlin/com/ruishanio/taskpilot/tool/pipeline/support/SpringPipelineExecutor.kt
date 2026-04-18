package com.ruishanio.taskpilot.tool.pipeline.support

import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.MapTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.pipeline.Pipeline
import com.ruishanio.taskpilot.tool.pipeline.PipelineExecutor
import com.ruishanio.taskpilot.tool.pipeline.PipelineHandler
import com.ruishanio.taskpilot.tool.pipeline.PipelineStatus
import com.ruishanio.taskpilot.tool.pipeline.config.PipelineConfig
import com.ruishanio.taskpilot.tool.pipeline.config.PipelineConfigLoader
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Spring 场景下的流水线执行器。
 * 初始化阶段继续按“加载配置 -> 找处理器 -> 按配置注册”三步走，方便和旧版日志逐项对照。
 */
class SpringPipelineExecutor :
    PipelineExecutor(),
    ApplicationContextAware,
    SmartInitializingSingleton,
    DisposableBean {
    private var applicationContext: ApplicationContext? = null
    private var pipelineConfigLoader: PipelineConfigLoader? = null

    fun setPipelineConfigLoader(pipelineConfigLoader: PipelineConfigLoader?) {
        this.pipelineConfigLoader = pipelineConfigLoader
    }

    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    override fun afterSingletonsInstantiated() {
        logger.info("SpringPipelineExecutor 开始初始化。")
        init()
    }

    /**
     * 初始化失败时直接返回 `false` 并写日志，维持原先的可观测性和短路路径。
     */
    private fun init(): Boolean {
        if (applicationContext == null) {
            logger.info("SpringPipelineExecutor 初始化失败，applicationContext 为空。")
            return false
        }
        if (pipelineConfigLoader == null) {
            logger.info("SpringPipelineExecutor 初始化失败，pipelineConfigLoader 为空。")
            return false
        }

        val configList = pipelineConfigLoader!!.loadAll()
        if (CollectionTool.isEmpty(configList)) {
            logger.info("SpringPipelineExecutor 初始化失败，未加载到流水线配置。")
            return false
        }

        val handlerMap = applicationContext!!.getBeansOfType(PipelineHandler::class.java)
        if (MapTool.isEmpty(handlerMap)) {
            logger.info("SpringPipelineExecutor 初始化失败，未找到流水线处理器。")
            return false
        }

        for (config in configList) {
            val ret = registryByConfig(config, handlerMap)
            logger.info("SpringPipelineExecutor 按配置注册流水线完成，result={}, config={}", ret, config)
        }
        logger.info("SpringPipelineExecutor 初始化完成。")
        return true
    }

    private fun registryByConfig(config: PipelineConfig, handlerMap: Map<String, PipelineHandler>): Boolean {
        if (StringTool.isBlank(config.name)) {
            logger.info("SpringPipelineExecutor 按配置注册流水线失败，名称为空，config={}", config)
            return false
        }
        if (CollectionTool.isEmpty(config.handlerList)) {
            logger.info("SpringPipelineExecutor 按配置注册流水线失败，handlerList 为空，config={}", config)
        }

        val pipeline = Pipeline()
            .name(config.name)
            .status(PipelineStatus.RUNTIME.status)

        for (handlerName in config.handlerList!!) {
            val handler = handlerMap[handlerName]
            if (handler == null) {
                logger.info("SpringPipelineExecutor 按配置注册流水线失败，未找到处理器，config={}, handlerName={}", config, handlerName)
                return false
            }
            pipeline.addLast(handler)
        }

        return registry(pipeline)
    }

    override fun destroy() {
        logger.info("SpringPipelineExecutor 已销毁。")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SpringPipelineExecutor::class.java)
    }
}
