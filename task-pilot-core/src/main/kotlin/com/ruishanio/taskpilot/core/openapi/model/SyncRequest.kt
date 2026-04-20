package com.ruishanio.taskpilot.core.openapi.model

import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum

/**
 * 执行器与任务定义同步请求。
 */
data class SyncRequest(
    /**
     * 执行器所属应用名。
     */
    var appName: String? = null,
    /**
     * 执行器分组标题。
     */
    var groupTitle: String? = null,
    /**
     * 需要同步到调度中心的任务列表。
     */
    var tasks: MutableList<Task> = mutableListOf()
) {
    /**
     * 单个任务的同步声明。
     */
    data class Task(
        /**
         * 与 `@TaskPilot` 注解中的 handler 对应，是同步查重和更新的稳定主键。
         */
        var executorHandler: String? = null,
        /**
         * 调度中心展示的任务标题，留空时回退为 executorHandler。
         */
        var jobDesc: String? = null,
        /**
         * 任务负责人，主要用于后台审计和告警展示。
         */
        var author: String? = null,
        /**
         * 单任务覆盖的报警邮箱，留空时允许由 Starter 默认值补齐。
         */
        var alarmEmail: String? = null,
        /**
         * 任务调度方式，决定 `scheduleConf` 应按 Cron 还是固定频率解析。
         */
        var scheduleType: ScheduleTypeEnum? = null,
        /**
         * 调度表达式内容，具体格式取决于 scheduleType。
         */
        var scheduleConf: String? = null,
        /**
         * 调度线程在任务过期时采用的补偿策略。
         */
        var misfireStrategy: MisfireStrategyEnum? = null,
        /**
         * 调度中心选择执行器地址时使用的路由策略。
         */
        var executorRouteStrategy: ExecutorRouteStrategyEnum? = null,
        /**
         * 调度中心透传给执行器的原始参数。
         */
        var executorParam: String? = null,
        /**
         * 执行器侧同一任务并发触发时的冲突处理策略。
         */
        var executorBlockStrategy: ExecutorBlockStrategyEnum? = null,
        /**
         * 单次执行超时时间，单位秒，0 表示沿用平台默认行为。
         */
        var executorTimeout: Int = 0,
        /**
         * 执行失败后的自动重试次数。
         */
        var executorFailRetryCount: Int = 0,
        /**
         * 当前任务成功后需要串联触发的子任务 ID 列表。
         */
        var childJobId: String? = null,
        /**
         * 首次同步创建任务后是否立即启动。
         */
        var autoStart: Boolean = false
    )
}
