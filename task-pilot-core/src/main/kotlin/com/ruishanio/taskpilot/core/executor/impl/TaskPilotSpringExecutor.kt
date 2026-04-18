package com.ruishanio.taskpilot.core.executor.impl

import com.ruishanio.taskpilot.core.executor.TaskPilotExecutor
import com.ruishanio.taskpilot.core.glue.GlueFactory
import com.ruishanio.taskpilot.core.handler.discovery.TaskPilotMethodScanner
import org.springframework.beans.BeansException
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Spring 场景下的 TaskPilot 执行器实现。
 */
class TaskPilotSpringExecutor :
    TaskPilotExecutor(),
    ApplicationContextAware,
    SmartInitializingSingleton,
    DisposableBean {
    /**
     * 需要排除的扫描包前缀，例如 `org.springframework`、`org.aaa,org.bbb`。
     */
    var excludedPackage: String = "org.springframework.,spring."

    /**
     * 在 Spring 完成单例 Bean 初始化后启动执行器。
     */
    override fun afterSingletonsInstantiated() {
        scanJobHandlerMethod(applicationContext)
        GlueFactory.refreshInstance(1)
        try {
            super.start()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun destroy() {
        super.destroy()
    }

    /**
     * 从 Spring 容器中扫描并注册任务方法。
     */
    private fun scanJobHandlerMethod(applicationContext: ApplicationContext?) {
        if (applicationContext == null) {
            return
        }

        for (definition in TaskPilotMethodScanner.scan(applicationContext, excludedPackage)) {
            registryJobHandler(definition.taskPilot(), definition.bean(), definition.method())
        }
    }

    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        Companion.applicationContext = applicationContext
    }

    companion object {
        private var applicationContext: ApplicationContext? = null

        /**
         * 获取当前 Spring 上下文，供 GLUE 注入时按需使用。
         */
        fun getApplicationContext(): ApplicationContext? = applicationContext
    }
}
