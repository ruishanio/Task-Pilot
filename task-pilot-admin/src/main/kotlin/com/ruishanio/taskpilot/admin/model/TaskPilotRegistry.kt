package com.ruishanio.taskpilot.admin.model

import java.util.Date

/**
 * 执行器注册表记录模型。
 */
data class TaskPilotRegistry(
    var id: Int = 0,
    var registryGroup: String? = null,
    var registryKey: String? = null,
    var registryValue: String? = null,
    var updateTime: Date? = null
)
