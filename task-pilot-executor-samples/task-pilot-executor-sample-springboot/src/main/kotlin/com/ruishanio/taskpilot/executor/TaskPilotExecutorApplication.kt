package com.ruishanio.taskpilot.executor

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * 示例执行器启动入口。
 */
@SpringBootApplication
class TaskPilotExecutorApplication

/**
 * 示例执行器采用顶层 `main`，保持 Kotlin 入口写法简洁直接。
 */
fun main(args: Array<String>) {
    SpringApplication.run(TaskPilotExecutorApplication::class.java, *args)
}
