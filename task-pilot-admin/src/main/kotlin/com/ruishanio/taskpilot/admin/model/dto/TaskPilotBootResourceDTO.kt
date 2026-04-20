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
    /**
     * 资源类型，当前启动菜单主要使用目录/页面两类轻量标识。
     */
    var type: Int = 0
    /**
     * 访问该资源所需的权限标识，为空表示所有登录用户可见。
     */
    var permission: String? = null
    var url: String? = null
    var icon: String? = null
    var order: Int = 0
    /**
     * 菜单状态，当前前端引导阶段主要保留字段兼容性。
     */
    var status: Int = 0
    var addTime: Date? = null
    var updateTime: Date? = null
    /**
     * 子资源列表，前端据此构建菜单树。
     */
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
