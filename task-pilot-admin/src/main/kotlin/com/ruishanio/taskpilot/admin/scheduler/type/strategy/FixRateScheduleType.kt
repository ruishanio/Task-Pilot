package com.ruishanio.taskpilot.admin.scheduler.type.strategy

import com.ruishanio.taskpilot.admin.model.TaskPilotInfo
import com.ruishanio.taskpilot.admin.scheduler.type.ScheduleType
import java.util.Date

/**
 * 按固定频率秒数计算下一次触发时间。
 */
class FixRateScheduleType : ScheduleType() {
    @Throws(Exception::class)
    override fun generateNextTriggerTime(jobInfo: TaskPilotInfo, fromTime: Date): Date =
        Date(fromTime.time + jobInfo.scheduleConf!!.toLong() * 1000L)
}
