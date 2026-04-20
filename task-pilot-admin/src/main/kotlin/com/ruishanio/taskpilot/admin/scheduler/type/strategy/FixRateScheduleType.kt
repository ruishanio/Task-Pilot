package com.ruishanio.taskpilot.admin.scheduler.type.strategy

import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.scheduler.type.ScheduleType
import java.util.Date

/**
 * 按固定频率秒数计算下一次触发时间。
 */
class FixRateScheduleType : ScheduleType() {
    @Throws(Exception::class)
    override fun generateNextTriggerTime(taskInfo: TaskInfo, fromTime: Date): Date =
        Date(fromTime.time + taskInfo.scheduleConf!!.toLong() * 1000L)
}
