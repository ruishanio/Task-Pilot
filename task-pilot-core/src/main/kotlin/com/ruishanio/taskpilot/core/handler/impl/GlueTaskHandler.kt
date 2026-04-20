package com.ruishanio.taskpilot.core.handler.impl

import com.ruishanio.taskpilot.core.context.TaskPilotHelper
import com.ruishanio.taskpilot.core.handler.ITaskHandler

/**
 * GLUE 包装处理器。
 *
 * 在实际任务执行前追加版本日志，便于定位脚本热更新后的执行来源。
 */
class GlueTaskHandler(
    private val taskHandler: ITaskHandler,
    private val glueUpdateTime: Long
) : ITaskHandler() {
    fun getGlueUpdateTime(): Long = glueUpdateTime

    @Throws(Exception::class)
    override fun execute() {
        TaskPilotHelper.log("----------- GLUE 版本:{} -----------", glueUpdateTime)
        taskHandler.execute()
    }

    @Throws(Exception::class)
    override fun init() {
        taskHandler.init()
    }

    @Throws(Exception::class)
    override fun destroy() {
        taskHandler.destroy()
    }
}
