package com.ruishanio.taskpilot.core.openapi.impl

import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.core.executor.TaskPilotExecutor
import com.ruishanio.taskpilot.core.glue.GlueFactory
import com.ruishanio.taskpilot.core.glue.GlueTypeEnum
import com.ruishanio.taskpilot.core.handler.IJobHandler
import com.ruishanio.taskpilot.core.handler.impl.GlueJobHandler
import com.ruishanio.taskpilot.core.handler.impl.ScriptJobHandler
import com.ruishanio.taskpilot.core.log.TaskPilotFileAppender
import com.ruishanio.taskpilot.core.openapi.ExecutorBiz
import com.ruishanio.taskpilot.core.openapi.model.IdleBeatRequest
import com.ruishanio.taskpilot.core.openapi.model.KillRequest
import com.ruishanio.taskpilot.core.openapi.model.LogRequest
import com.ruishanio.taskpilot.core.openapi.model.LogResult
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.core.thread.JobThread
import com.ruishanio.taskpilot.tool.response.Response
import org.slf4j.LoggerFactory
import java.util.Date

/**
 * 执行器远程调用实现。
 */
class ExecutorBizImpl : ExecutorBiz {
    override fun beat(): Response<String> = Response.ofSuccess()

    override fun idle(idleBeatRequest: IdleBeatRequest): Response<String> {
        val jobThread = TaskPilotExecutor.loadJobThread(idleBeatRequest.jobId)
        val isRunningOrHasQueue = jobThread != null && jobThread.isRunningOrHasQueue()
        return if (isRunningOrHasQueue) {
            Response.ofFail("job thread is running or has trigger queue.")
        } else {
            Response.ofSuccess()
        }
    }

    override fun run(triggerRequest: TriggerRequest): Response<String> {
        var jobThread = TaskPilotExecutor.loadJobThread(triggerRequest.jobId)
        var jobHandler: IJobHandler? = jobThread?.handler
        var removeOldReason: String? = null

        val glueTypeEnum = GlueTypeEnum.match(triggerRequest.glueType)
        if (GlueTypeEnum.BEAN == glueTypeEnum) {
            val newJobHandler = TaskPilotExecutor.loadJobHandler(triggerRequest.executorHandler ?: "")

            if (jobThread != null && jobHandler !== newJobHandler) {
                removeOldReason = "change jobhandler or glue type, and terminate the old job thread."
                jobThread = null
                jobHandler = null
            }

            if (jobHandler == null) {
                jobHandler = newJobHandler
                if (jobHandler == null) {
                    return Response.of(
                        TaskPilotContext.HANDLE_CODE_FAIL,
                        "job handler [${triggerRequest.executorHandler}] not found."
                    )
                }
            }
        } else if (GlueTypeEnum.GLUE_GROOVY == glueTypeEnum) {
            val glueJobHandler = jobThread?.handler as? GlueJobHandler
            if (jobThread != null &&
                glueJobHandler?.getGlueUpdateTime() != triggerRequest.glueUpdateTime
            ) {
                removeOldReason = "change job source or glue type, and terminate the old job thread."
                jobThread = null
                jobHandler = null
            }

            if (jobHandler == null) {
                try {
                    val originJobHandler = GlueFactory.getInstance().loadNewInstance(triggerRequest.glueSource)
                    jobHandler = GlueJobHandler(originJobHandler, triggerRequest.glueUpdateTime)
                } catch (e: Exception) {
                    logger.error("加载 GLUE 任务处理器时发生异常。jobId={}", triggerRequest.jobId, e)
                    return Response.of(TaskPilotContext.HANDLE_CODE_FAIL, e.message)
                }
            }
        } else if (glueTypeEnum != null && glueTypeEnum.isScript) {
            val scriptJobHandler = jobThread?.handler as? ScriptJobHandler
            if (jobThread != null &&
                scriptJobHandler?.getGlueUpdateTime() != triggerRequest.glueUpdateTime
            ) {
                removeOldReason = "change job source or glue type, and terminate the old job thread."
                jobThread = null
                jobHandler = null
            }

            if (jobHandler == null) {
                jobHandler =
                    ScriptJobHandler(
                        triggerRequest.jobId,
                        triggerRequest.glueUpdateTime,
                        triggerRequest.glueSource,
                        GlueTypeEnum.match(triggerRequest.glueType)
                    )
            }
        } else {
            return Response.of(TaskPilotContext.HANDLE_CODE_FAIL, "glueType[${triggerRequest.glueType}] is not valid.")
        }

        if (jobThread != null) {
            val blockStrategy = triggerRequest.executorBlockStrategy
            if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy) {
                if (jobThread.isRunningOrHasQueue()) {
                    return Response.of(
                        TaskPilotContext.HANDLE_CODE_FAIL,
                        "block strategy effect：" + ExecutorBlockStrategyEnum.DISCARD_LATER.title
                    )
                }
            } else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy) {
                if (jobThread.isRunningOrHasQueue()) {
                    removeOldReason = "block strategy effect：" + ExecutorBlockStrategyEnum.COVER_EARLY.title
                    jobThread = null
                }
            }
        }

        if (jobThread == null) {
            jobThread = TaskPilotExecutor.registJobThread(triggerRequest.jobId, jobHandler, removeOldReason)
        }

        return jobThread.pushTriggerQueue(triggerRequest)
    }

    override fun kill(killRequest: KillRequest): Response<String> {
        val jobThread: JobThread? = TaskPilotExecutor.loadJobThread(killRequest.jobId)
        return if (jobThread != null) {
            TaskPilotExecutor.removeJobThread(killRequest.jobId, "scheduling center kill job.")
            Response.ofSuccess()
        } else {
            Response.ofSuccess("job thread already killed.")
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
