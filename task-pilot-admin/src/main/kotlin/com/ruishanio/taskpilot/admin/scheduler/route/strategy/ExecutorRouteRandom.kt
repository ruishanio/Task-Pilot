package com.ruishanio.taskpilot.admin.scheduler.route.strategy

import com.ruishanio.taskpilot.admin.scheduler.route.ExecutorRouter
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.response.Response
import java.util.Random

/**
 * 随机选择一个执行器。
 */
class ExecutorRouteRandom : ExecutorRouter() {
    override fun route(triggerParam: TriggerRequest, addressList: List<String>): Response<String> =
        Response.ofSuccess(addressList[localRandom.nextInt(addressList.size)])

    companion object {
        private val localRandom = Random()
    }
}
