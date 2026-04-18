package com.ruishanio.taskpilot.core.openapi

import com.ruishanio.taskpilot.core.openapi.model.IdleBeatRequest
import com.ruishanio.taskpilot.core.openapi.model.KillRequest
import com.ruishanio.taskpilot.core.openapi.model.LogRequest
import com.ruishanio.taskpilot.core.openapi.model.LogResult
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.response.Response

/**
 * 执行器对外暴露的远程调用接口。
 */
interface ExecutorBiz {
    /**
     * 心跳探测。
     */
    fun beat(): Response<String>

    /**
     * 空闲检查。
     */
    fun idleBeat(idleBeatRequest: IdleBeatRequest): Response<String>

    /**
     * 触发任务执行。
     */
    fun run(triggerRequest: TriggerRequest): Response<String>

    /**
     * 终止任务执行。
     */
    fun kill(killRequest: KillRequest): Response<String>

    /**
     * 拉取执行日志。
     */
    fun log(logRequest: LogRequest): Response<LogResult>
}
