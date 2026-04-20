package com.ruishanio.taskpilot.admin.scheduler.alarm

import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.model.TaskLog

/**
 * 告警处理器抽象。
 */
interface TaskAlarm {
    /**
     * 对指定任务日志执行告警。
     */
    fun doAlarm(info: TaskInfo, taskLog: TaskLog): Boolean
}
