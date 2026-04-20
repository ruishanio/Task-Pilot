package com.ruishanio.taskpilot.admin.model

import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.enums.ExecutorRouteStrategyEnum
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum
import java.util.Date

/**
 * 任务定义模型。
 *
 * 该模型同时承载前端表单、调度计算和 MyBatis 持久化，字段命名尽量与数据库列保持一一对应。
 */
data class TaskPilotInfo(
    var id: Int = 0,
    /**
     * 执行器分组 ID，决定任务可以路由到哪一组执行器。
     */
    var jobGroup: Int = 0,
    var jobDesc: String? = null,
    var addTime: Date? = null,
    var updateTime: Date? = null,
    var author: String? = null,
    var alarmEmail: String? = null,
    /**
     * 调度类型决定 `scheduleConf` 的解析方式。
     */
    var scheduleType: ScheduleTypeEnum? = null,
    var scheduleConf: String? = null,
    /**
     * 调度线程发现任务过期时采用的补偿策略。
     */
    var misfireStrategy: MisfireStrategyEnum? = null,
    /**
     * 调度中心挑选执行器地址时使用的路由策略。
     */
    var executorRouteStrategy: ExecutorRouteStrategyEnum? = null,
    var executorHandler: String? = null,
    var executorParam: String? = null,
    /**
     * 执行器同一任务并发触发时的冲突处理策略。
     */
    var executorBlockStrategy: ExecutorBlockStrategyEnum? = null,
    var executorTimeout: Int = 0,
    var executorFailRetryCount: Int = 0,
    var glueType: String? = null,
    var glueSource: String? = null,
    var glueRemark: String? = null,
    var glueUpdatetime: Date? = null,
    /**
     * 子任务 ID 列表，按英文逗号分隔并在保存前做归一化。
     */
    var childJobId: String? = null,
    /**
     * 0 表示停止，1 表示运行。
     */
    var triggerStatus: Int = 0,
    /**
     * 最近一次调度时间戳，毫秒。
     */
    var triggerLastTime: Long = 0,
    /**
     * 下一次计划触发时间戳，毫秒。
     */
    var triggerNextTime: Long = 0
)
