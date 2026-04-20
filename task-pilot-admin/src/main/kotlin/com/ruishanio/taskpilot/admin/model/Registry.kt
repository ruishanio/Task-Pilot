package com.ruishanio.taskpilot.admin.model

import java.util.Date

/**
 * 执行器注册表记录模型。
 *
 * 注册中心依赖该表维护执行器在线地址，调度前会按心跳超时窗口过滤失活节点。
 */
data class Registry(
    var id: Int = 0,
    /**
     * 注册分组，当前主要使用 EXECUTOR。
     */
    var registryGroup: String? = null,
    /**
     * 注册键，执行器场景下一般是 appname。
     */
    var registryKey: String? = null,
    /**
     * 注册值，执行器场景下一般是可访问地址。
     */
    var registryValue: String? = null,
    var updateTime: Date? = null
)
