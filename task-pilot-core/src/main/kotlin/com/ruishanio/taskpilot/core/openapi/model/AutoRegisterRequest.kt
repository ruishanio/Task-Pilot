package com.ruishanio.taskpilot.core.openapi.model

import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum

/**
 * 执行器与任务自动注册请求。
 */
data class AutoRegisterRequest(
    /**
     * 执行器 AppName。
     */
    var appname: String? = null,
    /**
     * 执行器分组标题。
     */
    var title: String? = null,
    /**
     * 需要自动新增或同步更新的任务列表。
     */
    var tasks: MutableList<Task> = mutableListOf()
) {
    /**
     * 单个任务的自动注册声明。
     */
    data class Task(
        /**
         * 与 `@TaskPilot` 注解中的 handler 对应，是自动注册查重和更新的稳定主键。
         */
        var executorHandler: String? = null,
        var jobDesc: String? = null,
        var author: String? = null,
        var alarmEmail: String? = null,
        /**
         * 任务调度方式，决定 `scheduleConf` 应按 Cron 还是固定频率解析。
         */
        var scheduleType: ScheduleTypeEnum? = null,
        var scheduleConf: String? = null,
        /**
         * 调度线程在任务过期时采用的补偿策略。
         */
        var misfireStrategy: MisfireStrategyEnum? = null,
        /**
         * 调度中心选择执行器地址时使用的路由策略。
         */
        var executorRouteStrategy: ExecutorRouteStrategyEnum? = null,
        var executorParam: String? = null,
        /**
         * 执行器侧同一任务并发触发时的冲突处理策略。
         */
        var executorBlockStrategy: ExecutorBlockStrategyEnum? = null,
        var executorTimeout: Int = 0,
        var executorFailRetryCount: Int = 0,
        var childJobId: String? = null,
        var autoStart: Boolean = false
    )
}
