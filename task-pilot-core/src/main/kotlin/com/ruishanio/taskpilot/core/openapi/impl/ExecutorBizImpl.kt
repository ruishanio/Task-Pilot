package com.ruishanio.taskpilot.core.openapi.impl

import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.core.executor.TaskPilotExecutor
import com.ruishanio.taskpilot.core.glue.GlueFactory
import com.ruishanio.taskpilot.core.glue.GlueTypeEnum
import com.ruishanio.taskpilot.core.handler.ITaskHandler
import com.ruishanio.taskpilot.core.handler.impl.GlueTaskHandler
import com.ruishanio.taskpilot.core.handler.impl.ScriptTaskHandler
import com.ruishanio.taskpilot.core.log.TaskPilotFileAppender
import com.ruishanio.taskpilot.core.openapi.ExecutorBiz
import com.ruishanio.taskpilot.core.openapi.model.IdleBeatRequest
import com.ruishanio.taskpilot.core.openapi.model.KillRequest
import com.ruishanio.taskpilot.core.openapi.model.LogRequest
import com.ruishanio.taskpilot.core.openapi.model.LogResult
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.core.thread.TaskThread
import com.ruishanio.taskpilot.tool.response.Response
import org.slf4j.LoggerFactory
import java.util.Date

/**
 * 执行器远程调用实现。
 */
class ExecutorBizImpl : ExecutorBiz {
    override fun beat(): Response<String> = Response.ofSuccess()

    override fun idle(idleBeatRequest: IdleBeatRequest): Response<String> {
        val taskThread = TaskPilotExecutor.loadTaskThread(idleBeatRequest.taskId)
        val isRunningOrHasQueue = taskThread != null && taskThread.isRunningOrHasQueue()
        return if (isRunningOrHasQueue) {
            Response.ofFail("task thread is running or has trigger queue.")
        } else {
            Response.ofSuccess()
        }
    }

    override fun run(triggerRequest: TriggerRequest): Response<String> {
        var taskThread = TaskPilotExecutor.loadTaskThread(triggerRequest.taskId)
        var taskHandler: ITaskHandler? = taskThread?.handler
        var removeOldReason: String? = null

        val glueTypeEnum = GlueTypeEnum.match(triggerRequest.glueType)
        if (GlueTypeEnum.BEAN == glueTypeEnum) {
            val newTaskHandler = TaskPilotExecutor.loadTaskHandler(triggerRequest.executorHandler ?: "")

            if (taskThread != null && taskHandler !== newTaskHandler) {
                removeOldReason = "change taskhandler or glue type, and terminate the old task thread."
                taskThread = null
                taskHandler = null
            }

            if (taskHandler == null) {
                taskHandler = newTaskHandler
                if (taskHandler == null) {
                    return Response.of(
                        TaskPilotContext.HANDLE_CODE_FAIL,
                        "task handler [${triggerRequest.executorHandler}] not found."
                    )
                }
            }
        } else if (GlueTypeEnum.GLUE_GROOVY == glueTypeEnum) {
            val glueTaskHandler = taskThread?.handler as? GlueTaskHandler
            if (taskThread != null &&
                glueTaskHandler?.getGlueUpdateTime() != triggerRequest.glueUpdateTime
            ) {
                removeOldReason = "change task source or glue type, and terminate the old task thread."
                taskThread = null
                taskHandler = null
            }

            if (taskHandler == null) {
                try {
                    val originTaskHandler = GlueFactory.getInstance().loadNewInstance(triggerRequest.glueSource)
                    taskHandler = GlueTaskHandler(originTaskHandler, triggerRequest.glueUpdateTime)
                } catch (e: Exception) {
                    logger.error("加载 GLUE 任务处理器时发生异常。taskId={}", triggerRequest.taskId, e)
                    return Response.of(TaskPilotContext.HANDLE_CODE_FAIL, e.message)
                }
            }
        } else if (glueTypeEnum != null && glueTypeEnum.isScript) {
            val scriptTaskHandler = taskThread?.handler as? ScriptTaskHandler
            if (taskThread != null &&
                scriptTaskHandler?.getGlueUpdateTime() != triggerRequest.glueUpdateTime
            ) {
                removeOldReason = "change task source or glue type, and terminate the old task thread."
                taskThread = null
                taskHandler = null
            }

            if (taskHandler == null) {
                taskHandler =
                    ScriptTaskHandler(
                        triggerRequest.taskId,
                        triggerRequest.glueUpdateTime,
                        triggerRequest.glueSource,
                        GlueTypeEnum.match(triggerRequest.glueType)
                    )
            }
        } else {
            return Response.of(TaskPilotContext.HANDLE_CODE_FAIL, "glueType[${triggerRequest.glueType}] is not valid.")
        }

        if (taskThread != null) {
            val blockStrategy = triggerRequest.executorBlockStrategy
            if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy) {
                if (taskThread.isRunningOrHasQueue()) {
                    return Response.of(
                        TaskPilotContext.HANDLE_CODE_FAIL,
                        "block strategy effect：" + ExecutorBlockStrategyEnum.DISCARD_LATER.title
                    )
                }
            } else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy) {
                if (taskThread.isRunningOrHasQueue()) {
                    removeOldReason = "block strategy effect：" + ExecutorBlockStrategyEnum.COVER_EARLY.title
                    taskThread = null
                }
            }
        }

        if (taskThread == null) {
            taskThread = TaskPilotExecutor.registerTaskThread(triggerRequest.taskId, taskHandler, removeOldReason)
        }

        return taskThread.pushTriggerQueue(triggerRequest)
    }

    override fun kill(killRequest: KillRequest): Response<String> {
        val taskThread: TaskThread? = TaskPilotExecutor.loadTaskThread(killRequest.taskId)
        return if (taskThread != null) {
            TaskPilotExecutor.removeTaskThread(killRequest.taskId, "scheduling center kill task.")
            Response.ofSuccess()
        } else {
            Response.ofSuccess("task thread already killed.")
        }
    }

    override fun log(logRequest: LogRequest): Response<LogResult> {
        val logFileName = TaskPilotFileAppender.makeLogFileName(Date(logRequest.logDateTim), logRequest.logId)
        val logResult = TaskPilotFileAppender.readLog(logFileName, logRequest.fromLineNum)
        return Response.ofSuccess(logResult)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExecutorBizImpl::class.java)
    }
}
