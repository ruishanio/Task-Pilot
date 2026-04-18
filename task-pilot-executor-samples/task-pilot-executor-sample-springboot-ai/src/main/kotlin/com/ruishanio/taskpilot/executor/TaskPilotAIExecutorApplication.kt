package com.ruishanio.taskpilot.executor

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * AI 示例执行器启动入口。
 */
@SpringBootApplication
class TaskPilotAIExecutorApplication

/**
 * AI 示例执行器采用顶层 `main`，避免再维护仅为 JVM 入口服务的 companion。
 */
fun main(args: Array<String>) {
    SpringApplication.run(TaskPilotAIExecutorApplication::class.java, *args)
}
