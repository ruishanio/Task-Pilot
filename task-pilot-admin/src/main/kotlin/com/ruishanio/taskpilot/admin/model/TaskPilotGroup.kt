package com.ruishanio.taskpilot.admin.model

import com.ruishanio.taskpilot.tool.core.StringTool
import java.util.Date

/**
 * 执行器分组模型。
 */
data class TaskPilotGroup(
    var id: Int = 0,
    var appname: String? = null,
    var title: String? = null,
    var addressType: Int = 0,
    var addressList: String? = null,
    var updateTime: Date? = null
) {
    /**
     * 注册地址列表改成派生属性，避免模型本身再维护一份和 `addressList` 重复的可变状态。
     */
    val registryList: List<String>?
        get() = if (StringTool.isNotBlank(addressList)) addressList!!.split(",") else null
}
