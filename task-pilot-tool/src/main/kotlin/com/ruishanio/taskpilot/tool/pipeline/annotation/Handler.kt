package com.ruishanio.taskpilot.tool.pipeline.annotation

/**
 * Pipeline 处理器标识注解。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@java.lang.annotation.Inherited
annotation class Handler(val value: String)
