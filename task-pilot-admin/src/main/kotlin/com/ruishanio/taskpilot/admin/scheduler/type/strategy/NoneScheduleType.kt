package com.ruishanio.taskpilot.admin.scheduler.type.strategy

import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.scheduler.type.ScheduleType
import java.util.Date

/**
 * 不自动生成触发时间的占位调度类型。
 */
class NoneScheduleType : ScheduleType() {
    @Throws(Exception::class)
    override fun generateNextTriggerTime(jobInfo: TaskInfo, fromTime: Date): Date? = null
}
