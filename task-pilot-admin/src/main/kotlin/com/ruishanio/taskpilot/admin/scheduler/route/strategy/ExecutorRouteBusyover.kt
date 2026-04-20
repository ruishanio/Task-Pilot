package com.ruishanio.taskpilot.admin.scheduler.route.strategy

import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.route.ExecutorRouter
import com.ruishanio.taskpilot.core.openapi.model.IdleBeatRequest
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.response.Response

/**
 * 忙碌转移路由。
 *
 * 逐个检测执行器空闲状态，命中可执行节点后立即返回，并保留完整探测链路消息。
 */
class ExecutorRouteBusyover : ExecutorRouter() {
    override fun route(triggerParam: TriggerRequest, addressList: List<String>): Response<String> {
        val idleBeatResultBuilder = StringBuilder()
        for (address in addressList) {
            val idleBeatResult = try {
                val executorBiz = TaskPilotAdminBootstrap.getExecutorBiz(address)
                executorBiz?.idle(IdleBeatRequest(triggerParam.taskId)) ?: Response.ofFail("executorBiz is null")
            } catch (e: Exception) {
                logger.error("忙碌转移路由空闲检测时发生异常，address={}", address, e)
                Response.ofFail("$e")
            }

            if (idleBeatResultBuilder.isNotEmpty()) {
                idleBeatResultBuilder.append("<br><br>")
            }
            idleBeatResultBuilder.append("空闲检测")
                .append("：")
                .append("<br>地址：").append(address)
                .append("<br>状态码：").append(idleBeatResult.code)
                .append("<br>消息：").append(idleBeatResult.msg)

            if (idleBeatResult.isSuccess) {
                idleBeatResult.msg = idleBeatResultBuilder.toString()
                idleBeatResult.data = address
                return idleBeatResult
            }
        }

        return Response.ofFail(idleBeatResultBuilder.toString())
    }
}
