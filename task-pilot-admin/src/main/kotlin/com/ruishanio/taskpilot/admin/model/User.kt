package com.ruishanio.taskpilot.admin.model

/**
 * 管理端用户模型。
 *
 * 当前仍是较轻量的本地认证模型，权限数据直接保存在用户表中。
 */
class User {
    var id: Int = 0
    var username: String? = null
    var password: String? = null
    var token: String? = null
    /**
     * 角色：0=普通用户，1=管理员。
     */
    var role: Int = 0
    /**
     * 可访问的执行器分组 ID 列表，多个值以英文逗号分隔。
     */
    var permission: String? = null
}
