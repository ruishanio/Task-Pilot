package com.ruishanio.taskpilot.admin.controller.api

import com.ruishanio.taskpilot.admin.service.TaskPilotService
import com.ruishanio.taskpilot.admin.web.ManageRoute
import com.ruishanio.taskpilot.tool.response.Response
import jakarta.annotation.Resource
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 管理端统计聚合接口。
 *
 * 将仪表盘摘要统计统一挂到 `/stat` 前缀，便于和系统初始化类接口拆分职责。
 */
@RestController
@RequestMapping(ManageRoute.API_MANAGE_STAT)
class StatApiController {
    @Resource
    private lateinit var taskPilotService: TaskPilotService

    /**
     * Dashboard 统计直接复用服务层聚合结果，避免前后端接口分叉出两套统计口径。
     */
    @RequestMapping("/dashboard")
    fun dashboard(): Response<Map<String, Any>> = Response.ofSuccess(taskPilotService.dashboardInfo())
}
