package com.ruishanio.taskpilot.core.openapi

import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.openapi.model.CallbackRequest
import com.ruishanio.taskpilot.core.openapi.model.RegistryRequest
import com.ruishanio.taskpilot.core.openapi.model.SyncRequest
import com.ruishanio.taskpilot.tool.response.Response
import com.ruishanio.taskpilot.tool.http.client.HttpClientService

/**
 * 调度中心暴露给执行器的远程接口。
 */
@HttpClientService(path = Const.ADMIN_OPEN_API_PREFIX)
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
     * 同步执行器分组与任务定义。
     */
    fun sync(syncRequest: SyncRequest): Response<String>
}
