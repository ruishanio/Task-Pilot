package com.ruishanio.taskpilot.tool.pipeline

import com.ruishanio.taskpilot.tool.response.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * 流水线上下文。
 * 继续允许处理器通过共享上下文和响应对象协作，并用 `isBreak` 控制后续链路是否终止。
 */
class PipelineContext() {
    var request: Any? = null
    var response: Response<Any>? = Response.ofSuccess()
    var isBreak: Boolean = false
    var contextMap: ConcurrentMap<String, Any> = ConcurrentHashMap()

    constructor(request: Any?) : this() {
        this.request = request
    }

    override fun toString(): String =
        "PipelineContext{request=$request, response=$response, isBreak=$isBreak, contextMap=$contextMap}"

    fun breakToFail() {
        breakToFail(Response.ofFail())
    }

    /**
     * 失败中断直接覆盖当前响应，保持旧版处理器可以在任意节点短路整条流水线。
     */
    fun breakToFail(response: Response<Any>?) {
        isBreak = true
        this.response = response
    }
}
