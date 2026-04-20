package com.ruishanio.taskpilot.admin.service.impl

import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.core.openapi.AdminBiz
import com.ruishanio.taskpilot.core.openapi.model.CallbackRequest
import com.ruishanio.taskpilot.core.openapi.model.RegistryRequest
import com.ruishanio.taskpilot.core.openapi.model.SyncRequest
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import org.springframework.stereotype.Service

/**
 * Admin OpenAPI 默认实现。
 *
 * 继续把 callback/registry 交给线程组件处理，任务定义同步能力则收敛到专用服务，避免协议适配层侵入业务细节。
 */
@Service
class AdminBizImpl : AdminBiz {
    @Resource
    private lateinit var taskPilotSyncService: TaskPilotSyncService

    override fun callback(callbackRequestList: List<CallbackRequest>): Response<String> =
        TaskPilotAdminBootstrap.instance.jobCompleteHelper.callback(callbackRequestList)

    override fun registry(registryRequest: RegistryRequest): Response<String> =
        TaskPilotAdminBootstrap.instance.jobRegistryHelper.registry(registryRequest)

    override fun registryRemove(registryRequest: RegistryRequest): Response<String> =
        TaskPilotAdminBootstrap.instance.jobRegistryHelper.registryRemove(registryRequest)

    override fun sync(syncRequest: SyncRequest): Response<String> =
        taskPilotSyncService.sync(syncRequest)
}
