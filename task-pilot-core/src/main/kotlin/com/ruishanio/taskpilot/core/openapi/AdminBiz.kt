package com.ruishanio.taskpilot.core.openapi

import com.ruishanio.taskpilot.core.openapi.model.AutoRegisterRequest
import com.ruishanio.taskpilot.core.openapi.model.CallbackRequest
import com.ruishanio.taskpilot.core.openapi.model.RegistryRequest
import com.ruishanio.taskpilot.tool.response.Response

/**
 * 调度中心对执行器暴露的管理接口。
 */
interface AdminBiz {
    /**
     * 回调执行结果。
     */
    fun callback(callbackRequestList: List<CallbackRequest>): Response<String>

    /**
     * 注册执行器节点。
     */
    fun registry(registryRequest: RegistryRequest): Response<String>

    /**
     * 注销执行器节点。
     */
    fun registryRemove(registryRequest: RegistryRequest): Response<String>

    /**
     * 自动注册执行器分组与任务定义。
     */
    fun autoRegister(autoRegisterRequest: AutoRegisterRequest): Response<String>
}
