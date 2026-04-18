package com.ruishanio.taskpilot.admin.scheduler.route

import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.response.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 执行器路由器抽象。
 */
abstract class ExecutorRouter {
    protected val logger: Logger = LoggerFactory.getLogger(ExecutorRouter::class.java)

    /**
     * 从候选地址中选出最终执行器。
     */
    abstract fun route(triggerParam: TriggerRequest, addressList: List<String>): Response<String>
}
