package com.ruishanio.taskpilot.admin.scheduler.alarm

import com.ruishanio.taskpilot.admin.model.TaskPilotInfo
import com.ruishanio.taskpilot.admin.model.TaskPilotLog

/**
 * 告警处理器抽象。
 */
interface JobAlarm {
    /**
     * 对指定任务日志执行告警。
     */
    fun doAlarm(info: TaskPilotInfo, jobLog: TaskPilotLog): Boolean
}
