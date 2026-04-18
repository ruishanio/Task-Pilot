package com.ruishanio.taskpilot.admin.scheduler.route.strategy

import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.route.ExecutorRouter
import com.ruishanio.taskpilot.admin.util.I18nUtil
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.response.Response

/**
 * 故障转移路由。
 *
 * 逐个探活候选执行器，首个心跳成功的节点立即返回，同时把探测轨迹写入响应消息便于排障。
 */
class ExecutorRouteFailover : ExecutorRouter() {
    override fun route(triggerParam: TriggerRequest, addressList: List<String>): Response<String> {
        val beatResultBuilder = StringBuilder()
        for (address in addressList) {
            val beatResult = try {
                val executorBiz = TaskPilotAdminBootstrap.getExecutorBiz(address)
                executorBiz?.beat() ?: Response.ofFail("executorBiz is null")
            } catch (e: Exception) {
                logger.error("故障转移路由心跳检测时发生异常，address={}", address, e)
                Response.ofFail(e.message)
            }

            if (beatResultBuilder.isNotEmpty()) {
                beatResultBuilder.append("<br><br>")
            }
            beatResultBuilder.append(I18nUtil.getString("jobconf_beat"))
                .append("：")
                .append("<br>地址：").append(address)
                .append("<br>状态码：").append(beatResult.code)
                .append("<br>消息：").append(beatResult.msg)

            if (beatResult.isSuccess) {
                beatResult.msg = beatResultBuilder.toString()
                beatResult.data = address
                return beatResult
            }
        }
        return Response.ofFail(beatResultBuilder.toString())
    }
}
