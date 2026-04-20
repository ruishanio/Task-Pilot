package com.ruishanio.taskpilot.spring.boot.starter.autoconfigure

import java.io.File
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * TaskPilot Spring Boot Starter 配置项。
 *
 * 说明：
 * 1、统一使用 task-pilot.admin.* / task-pilot.executor.* / task-pilot.sync.* 配置结构；
 * 2、执行器负责对外暴露运行能力，sync 负责把注解声明同步到调度中心。
 */
@ConfigurationProperties(prefix = "task-pilot")
class TaskPilotProperties {
    @field:NestedConfigurationProperty
    val admin: Admin = Admin()

    @field:NestedConfigurationProperty
    val executor: Executor = Executor()

    @field:NestedConfigurationProperty
    val sync: Sync = Sync()

    /**
     * 管理端连接配置。
     */
    class Admin {
        var addresses: String? = null
        var accessToken: String? = null
        var timeout: Int = 3
    }

    /**
     * 执行器运行配置。
     */
    class Executor {
        var enabled: Boolean = true
        var appname: String? = null
        var address: String = ""
        var ip: String = ""
        var port: Int = 9999
        var logpath: String = File(System.getProperty("user.home"), "logs/task-pilot/jobhandler").path
        var logretentiondays: Int = 30
        var excludedpackage: String = ""
    }

    /**
     * 启动时同步执行器与任务定义的配置。
     */
    class Sync {
        /**
         * 是否开启启动同步。
         */
        var enabled: Boolean = false

        /**
         * 自动创建执行器分组时使用的标题，留空时回退为 executor.appname。
         */
        var executorTitle: String = ""

        /**
         * 注解未显式声明 author 时使用的默认负责人。
         */
        var defaultTaskAuthor: String = "TASK-PILOT"

        /**
         * 注解未显式声明 alarmEmail 时使用的默认报警邮箱。
         */
        var defaultTaskAlarmEmail: String = ""
    }
}
