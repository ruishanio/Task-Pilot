package com.ruishanio.taskpilot.core.handler.annotation

import java.lang.annotation.Inherited

/**
 * TaskPilot 任务方法注解。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class TaskPilot(
    /**
     * 任务处理器名称，对应调度中心中的执行器处理器字段。
     */
    val value: String,
    /**
     * 任务线程初始化回调方法名。
     */
    val init: String = "",
    /**
     * 任务线程销毁回调方法名。
     */
    val destroy: String = ""
)
