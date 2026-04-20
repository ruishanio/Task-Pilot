package com.ruishanio.taskpilot.admin.model

import java.util.Date

/**
 * GLUE 代码变更日志模型。
 *
 * 每次修改脚本类任务源码时都会落一条快照，便于回溯与回滚。
 */
data class GlueLog(
    var id: Int = 0,
    var taskId: Int = 0,
    var glueType: String? = null,
    var glueSource: String? = null,
    var glueRemark: String? = null,
    var addTime: Date? = null,
    var updateTime: Date? = null
)
