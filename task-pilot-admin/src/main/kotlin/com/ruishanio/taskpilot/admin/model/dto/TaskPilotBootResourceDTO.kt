package com.ruishanio.taskpilot.admin.model.dto

import java.io.Serializable
import java.util.Date

/**
 * 启动页菜单资源 DTO。
 *
 * 继续提供无参构造和全参构造，兼容服务端模板渲染与前端引导接口的旧调用方式。
 */
class TaskPilotBootResourceDTO() : Serializable {
    var id: Int = 0
    var parentId: Int = 0
    var name: String? = null
    var type: Int = 0
    var permission: String? = null
    var url: String? = null
    var icon: String? = null
    var order: Int = 0
    var status: Int = 0
    var addTime: Date? = null
    var updateTime: Date? = null
    var children: List<TaskPilotBootResourceDTO>? = null

    constructor(
        id: Int,
        parentId: Int,
        name: String?,
        type: Int,
        permission: String?,
        url: String?,
        icon: String?,
        order: Int,
        status: Int,
        children: List<TaskPilotBootResourceDTO>?
    ) : this() {
        this.id = id
        this.parentId = parentId
        this.name = name
        this.type = type
        this.permission = permission
        this.url = url
        this.icon = icon
        this.order = order
        this.status = status
        this.children = children
    }

    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
