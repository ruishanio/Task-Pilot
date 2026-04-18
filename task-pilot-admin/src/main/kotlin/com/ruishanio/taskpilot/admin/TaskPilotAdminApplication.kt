package com.ruishanio.taskpilot.admin

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * 管理端应用入口。
 */
@SpringBootApplication
class TaskPilotAdminApplication

/**
 * 使用顶层 `main` 作为 Kotlin 应用入口，保持启动代码简洁直接。
 */
fun main(args: Array<String>) {
    SpringApplication.run(TaskPilotAdminApplication::class.java, *args)
}
