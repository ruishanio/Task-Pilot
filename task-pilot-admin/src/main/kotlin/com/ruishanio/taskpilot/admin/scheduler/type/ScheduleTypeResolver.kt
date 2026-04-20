package com.ruishanio.taskpilot.admin.scheduler.type

import com.ruishanio.taskpilot.admin.scheduler.type.strategy.CronScheduleType
import com.ruishanio.taskpilot.admin.scheduler.type.strategy.FixRateScheduleType
import com.ruishanio.taskpilot.admin.scheduler.type.strategy.NoneScheduleType
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum

/**
 * 把共享枚举适配为管理端实际使用的调度时间计算器，避免 core 反向依赖 admin 实现。
 */
fun ScheduleTypeEnum.toScheduleType(): ScheduleType =
    when (this) {
        ScheduleTypeEnum.NONE -> NONE_SCHEDULE_TYPE
        ScheduleTypeEnum.CRON -> CRON_SCHEDULE_TYPE
        ScheduleTypeEnum.FIX_RATE -> FIX_RATE_SCHEDULE_TYPE
    }

/**
 * 调度类型实现都是无状态对象，复用单例可避免调度热点路径反复创建实例。
 */
private val NONE_SCHEDULE_TYPE = NoneScheduleType()
private val CRON_SCHEDULE_TYPE = CronScheduleType()
private val FIX_RATE_SCHEDULE_TYPE = FixRateScheduleType()
