package com.ruishanio.taskpilot.admin.model

import java.util.Date

/**
 * 任务执行日志模型。
 *
 * 一次调度对应一条日志主记录，触发阶段与执行阶段的结果都会回填到这里。
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
    /**
     * 触发阶段返回码，通常对应调度中心侧是否成功下发。
     */
    var triggerCode: Int = 0,
    var triggerMsg: String? = null,
    var handleTime: Date? = null,
    /**
     * 执行阶段返回码，通常对应执行器回报的任务结果。
     */
    var handleCode: Int = 0,
    var handleMsg: String? = null,
    /**
     * 告警状态：0=默认，1=无需告警，2=告警成功，3=告警失败。
     */
    var alarmStatus: Int = 0
)
