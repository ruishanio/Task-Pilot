package com.ruishanio.taskpilot.admin.model

/**
 * 管理端用户模型。
 */
class TaskPilotUser {
    var id: Int = 0
    var username: String? = null
    var password: String? = null
    var token: String? = null
    var role: Int = 0
    var permission: String? = null
}
