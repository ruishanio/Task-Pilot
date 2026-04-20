package com.ruishanio.taskpilot.admin.scheduler.type.strategy

import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.scheduler.cron.CronExpression
import com.ruishanio.taskpilot.admin.scheduler.type.ScheduleType
import java.util.Date

/**
 * 基于 Cron 表达式计算下一次触发时间。
 */
class CronScheduleType : ScheduleType() {
    @Throws(Exception::class)
    override fun generateNextTriggerTime(jobInfo: TaskInfo, fromTime: Date): Date? =
        CronExpression(jobInfo.scheduleConf).getNextValidTimeAfter(fromTime)
}
