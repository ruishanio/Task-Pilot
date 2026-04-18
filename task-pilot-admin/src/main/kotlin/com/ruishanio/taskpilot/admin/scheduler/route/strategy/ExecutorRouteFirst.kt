package com.ruishanio.taskpilot.admin.scheduler.route.strategy

import com.ruishanio.taskpilot.admin.scheduler.route.ExecutorRouter
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.response.Response

/**
 * 固定选择第一个可用执行器。
 */
class ExecutorRouteFirst : ExecutorRouter() {
    override fun route(triggerParam: TriggerRequest, addressList: List<String>): Response<String> =
        Response.ofSuccess(addressList[0])
}
