package com.ruishanio.taskpilot.admin.model

import java.util.Date

/**
 * GLUE 代码变更日志模型。
 */
data class TaskPilotLogGlue(
    var id: Int = 0,
    var jobId: Int = 0,
    var glueType: String? = null,
    var glueSource: String? = null,
    var glueRemark: String? = null,
    var addTime: Date? = null,
    var updateTime: Date? = null
)
