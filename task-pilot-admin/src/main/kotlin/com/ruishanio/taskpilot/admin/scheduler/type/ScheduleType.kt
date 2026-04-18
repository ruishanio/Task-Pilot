package com.ruishanio.taskpilot.admin.scheduler.type

import com.ruishanio.taskpilot.admin.model.TaskPilotInfo
import java.util.Date

/**
 * 调度时间计算抽象。
 */
abstract class ScheduleType {
    /**
     * 基于任务配置和参考时间计算下一次触发时间。
     */
    @Throws(Exception::class)
    abstract fun generateNextTriggerTime(jobInfo: TaskPilotInfo, fromTime: Date): Date?
}
