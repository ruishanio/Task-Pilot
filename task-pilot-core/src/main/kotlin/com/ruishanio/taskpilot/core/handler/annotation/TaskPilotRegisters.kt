package com.ruishanio.taskpilot.core.handler.annotation

import java.lang.annotation.Inherited

/**
 * `@TaskPilotRegister` 的容器注解。
 *
 * 允许同一个执行方法声明多份调度注册配置，以便复用同一份执行逻辑生成多个任务定义。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class TaskPilotRegisters(
    val value: Array<TaskPilotRegister>
)
