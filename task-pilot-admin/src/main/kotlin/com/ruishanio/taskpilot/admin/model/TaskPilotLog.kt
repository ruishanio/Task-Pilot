package com.ruishanio.taskpilot.admin.model

import java.util.Date

/**
 * 任务执行日志模型。
 */
data class TaskPilotLog(
    var id: Long = 0,
    var jobGroup: Int = 0,
    var jobId: Int = 0,
    var executorAddress: String? = null,
    var executorHandler: String? = null,
    var executorParam: String? = null,
    var executorShardingParam: String? = null,
    var executorFailRetryCount: Int = 0,
    var triggerTime: Date? = null,
    var triggerCode: Int = 0,
    var triggerMsg: String? = null,
    var handleTime: Date? = null,
    var handleCode: Int = 0,
    var handleMsg: String? = null,
    var alarmStatus: Int = 0
)
