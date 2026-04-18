package com.ruishanio.taskpilot.spring.boot.starter.autoconfigure

import com.ruishanio.taskpilot.core.executor.impl.TaskPilotSpringExecutor
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.util.StringUtils

/**
 * TaskPilot 自动配置。
 *
 * 设计取舍：
 * 1、仅在显式启用 task-pilot.executor.enabled=true 时装配，避免引入 Starter 后无配置也被强制启动；
 * 2、对 admin.addresses 和 executor.appname 做最小必要校验，尽早暴露配置缺失问题。
 */
@AutoConfiguration
@ConditionalOnClass(TaskPilotSpringExecutor::class)
@EnableConfigurationProperties(TaskPilotProperties::class)
class TaskPilotAutoConfiguration {
    /**
     * 创建 Spring 环境下的 TaskPilot 执行器。
     */
    @Bean
    @ConditionalOnMissingBean(TaskPilotSpringExecutor::class)
    @ConditionalOnProperty(prefix = "task-pilot.executor", name = ["enabled"], havingValue = "true")
    fun taskPilotSpringExecutor(properties: TaskPilotProperties): TaskPilotSpringExecutor {
        validateRequiredProperties(properties)

        return TaskPilotSpringExecutor().apply {
            adminAddresses = properties.admin.addresses
            accessToken = properties.admin.accessToken
            timeout = properties.admin.timeout
            enabled = properties.executor.enabled
            appname = properties.executor.appname
            address = properties.executor.address
            ip = properties.executor.ip
            port = properties.executor.port
            logPath = properties.executor.logpath
            logRetentionDays = properties.executor.logretentiondays
            excludedPackage = properties.executor.excludedpackage
        }.also {
            logger.info(
                ">>>>>>>>>>> task-pilot Spring Boot Starter 自动配置完成。appname={}",
                properties.executor.appname
            )
        }
    }

    /**
     * 启动后把执行器分组与任务定义同步到调度中心。
     */
    @Bean
    @ConditionalOnBean(TaskPilotSpringExecutor::class)
    @ConditionalOnProperty(prefix = "task-pilot.auto-register", name = ["enabled"], havingValue = "true")
    fun taskPilotAutoRegisterRunner(
        properties: TaskPilotProperties,
        applicationContext: ApplicationContext
    ): ApplicationRunner = TaskPilotAutoRegisterRunner(properties, applicationContext)

    /**
     * 对启用场景做必要的配置校验，避免执行器启动后才输出模糊告警。
     */
    private fun validateRequiredProperties(properties: TaskPilotProperties) {
        if (!StringUtils.hasText(properties.admin.addresses)) {
            throw IllegalStateException("启用 TaskPilot 执行器时必须配置 task-pilot.admin.addresses。")
        }
        if (!StringUtils.hasText(properties.executor.appname)) {
            throw IllegalStateException("启用 TaskPilot 执行器时必须配置 task-pilot.executor.appname。")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskPilotAutoConfiguration::class.java)
    }
}
