package com.ruishanio.taskpilot.admin.model

import com.ruishanio.taskpilot.tool.core.StringTool
import java.util.Date

/**
 * 执行器分组模型。
 *
 * 一个分组对应一个执行器 appname，调度任务时会在该分组的地址池里做路由。
 */
data class TaskPilotGroup(
    var id: Int = 0,
    /**
     * 执行器注册时上报的 appname，也是自动建组时的唯一键。
     */
    var appname: String? = null,
    var title: String? = null,
    /**
     * 地址类型：0=自动注册，1=手动录入。
     */
    var addressType: Int = 0,
    /**
     * 当前可用地址列表，多个地址以英文逗号拼接。
     */
    var addressList: String? = null,
    var updateTime: Date? = null
) {
    /**
     * 注册地址列表改成派生属性，避免模型本身再维护一份和 `addressList` 重复的可变状态。
     */
    val registryList: List<String>?
        get() = if (StringTool.isNotBlank(addressList)) addressList!!.split(",") else null
}
