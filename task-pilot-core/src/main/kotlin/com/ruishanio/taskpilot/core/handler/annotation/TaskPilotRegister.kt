package com.ruishanio.taskpilot.core.handler.annotation

import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum
import java.lang.annotation.Inherited

/**
 * TaskPilot 自动注册注解。
 *
 * 设计意图：
 * 1、把调度中心注册元数据与执行方法定义解耦；
 * 2、只有同时声明 `@TaskPilot` 与本注解的方法，才会参与自动同步；
 * 3、重启时以代码中的注解配置为准，保证任务定义可随代码演进。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class TaskPilotRegister(
    /**
     * 自动注册到调度中心时使用的任务描述。
     */
    val jobDesc: String = "",
    /**
     * 自动注册到调度中心时使用的负责人。
     */
    val author: String = "",
    /**
     * 自动注册到调度中心时使用的告警邮箱。
     */
    val alarmEmail: String = "",
    /**
     * 自动注册到调度中心时使用的调度类型。
     */
    val scheduleType: ScheduleTypeEnum = ScheduleTypeEnum.CRON,
    /**
     * 自动注册到调度中心时使用的调度配置。
     */
    val scheduleConf: String = "",
    /**
     * 自动注册到调度中心时使用的调度过期策略。
     */
    val misfireStrategy: MisfireStrategyEnum = MisfireStrategyEnum.DO_NOTHING,
    /**
     * 自动注册到调度中心时使用的路由策略。
     */
    val executorRouteStrategy: ExecutorRouteStrategyEnum = ExecutorRouteStrategyEnum.FIRST,
    /**
     * 自动注册到调度中心时使用的默认任务参数。
     */
    val executorParam: String = "",
    /**
     * 自动注册到调度中心时使用的阻塞策略。
     */
    val executorBlockStrategy: ExecutorBlockStrategyEnum = ExecutorBlockStrategyEnum.SERIAL_EXECUTION,
    /**
     * 自动注册到调度中心时使用的执行超时时间，单位秒。
     */
    val executorTimeout: Int = 0,
    /**
     * 自动注册到调度中心时使用的失败重试次数。
     */
    val executorFailRetryCount: Int = 0,
    /**
     * 自动注册到调度中心时使用的子任务 ID 列表，多个逗号分隔。
     */
    val childJobId: String = "",
    /**
     * 自动新增任务后是否立即启动。
     */
    val autoStart: Boolean = false
)
