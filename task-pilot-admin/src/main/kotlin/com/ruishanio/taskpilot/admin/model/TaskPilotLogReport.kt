package com.ruishanio.taskpilot.admin.model

import java.util.Date

/**
 * 日志统计汇总模型。
 *
 * 该表按天聚合触发结果，用于 Dashboard 图表而不是明细查询。
 */
data class TaskPilotLogReport(
    var id: Int = 0,
    var triggerDay: Date? = null,
    var runningCount: Int = 0,
    var sucCount: Int = 0,
    var failCount: Int = 0
)
