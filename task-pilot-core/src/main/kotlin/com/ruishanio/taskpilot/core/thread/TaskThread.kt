package com.ruishanio.taskpilot.core.thread

import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.core.context.TaskPilotHelper
import com.ruishanio.taskpilot.core.executor.TaskPilotExecutor
import com.ruishanio.taskpilot.core.handler.ITaskHandler
import com.ruishanio.taskpilot.core.log.TaskPilotFileAppender
import com.ruishanio.taskpilot.core.openapi.model.CallbackRequest
import com.ruishanio.taskpilot.core.openapi.model.TriggerRequest
import com.ruishanio.taskpilot.tool.response.Response
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 单任务执行线程。
 *
 * 负责串行消费同一任务的触发请求，并在执行结束后统一回调调度中心。
 */
class TaskThread(
    private val taskId: Int,
    val handler: ITaskHandler
) : Thread() {
    private val triggerQueue = LinkedBlockingQueue<TriggerRequest>()
    private val triggerLogIdSet: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var toStop: Boolean = false
    private var stopReason: String? = null

    private var running: Boolean = false
    private var idleTimes: Int = 0

    init {
        name = "task-pilot, TaskThread-$taskId-${System.currentTimeMillis()}"
    }

    /**
     * 推送新的触发请求到队列。
     */
    fun pushTriggerQueue(triggerParam: TriggerRequest): Response<String> {
        if (!triggerLogIdSet.add(triggerParam.logId)) {
            logger.info(">>>>>>>>>>> 检测到重复触发任务，logId:{}", triggerParam.logId)
            return Response.of(TaskPilotContext.HANDLE_CODE_FAIL, "repeate trigger job, logId:${triggerParam.logId}")
        }

        triggerQueue.add(triggerParam)
        return Response.ofSuccess()
    }

    /**
     * 标记线程停止。
     */
    fun toStop(stopReason: String?) {
        this.toStop = true
        this.stopReason = stopReason
    }

    /**
     * 当前是否仍在执行或待执行。
     */
    fun isRunningOrHasQueue(): Boolean = running || triggerQueue.isNotEmpty()

    override fun run() {
        try {
            handler.init()
        } catch (e: Throwable) {
            logger.error(">>>>>>>>>>> 初始化任务处理器时发生异常。taskId={}", taskId, e)
        }

        while (!toStop) {
            running = false
            idleTimes++

            var triggerParam: TriggerRequest? = null
            try {
                triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS)
                if (triggerParam != null) {
                    running = true
                    idleTimes = 0
                    triggerLogIdSet.remove(triggerParam.logId)

                    val logFileName =
                        TaskPilotFileAppender.makeLogFileName(Date(triggerParam.logDateTime), triggerParam.logId)
                    val taskPilotContext =
                        TaskPilotContext(
                            triggerParam.taskId.toLong(),
                            triggerParam.executorParam,
                            triggerParam.logId,
                            triggerParam.logDateTime,
                            logFileName,
                            triggerParam.broadcastIndex,
                            triggerParam.broadcastTotal
                        )
                    TaskPilotContext.setTaskPilotContext(taskPilotContext)

                    TaskPilotHelper.log("<br>----------- task-pilot 任务执行开始 -----------<br>----------- 参数:${taskPilotContext.taskParam}")

                    if (triggerParam.executorTimeout > 0) {
                        var futureThread: Thread? = null
                        try {
                            val futureTask =
                                FutureTask(
                                    Callable<Boolean> {
                                        TaskPilotContext.setTaskPilotContext(taskPilotContext)
                                        handler.execute()
                                        true
                                    }
                                )
                            futureThread = Thread(futureTask)
                            futureThread.start()
                            futureTask.get(triggerParam.executorTimeout.toLong(), TimeUnit.SECONDS)
                        } catch (e: TimeoutException) {
                            TaskPilotHelper.log("<br>----------- task-pilot 任务执行超时")
                            TaskPilotHelper.log(e)
                            TaskPilotHelper.handleTimeout("任务执行超时")
                        } finally {
                            futureThread?.interrupt()
                        }
                    } else {
                        handler.execute()
                    }

                    val currentContext = TaskPilotContext.getTaskPilotContext()
                    if (currentContext == null || currentContext.handleCode <= 0) {
                        TaskPilotHelper.handleFail("任务处理结果缺失。")
                    } else {
                        var tempHandleMsg = currentContext.handleMsg
                        if (tempHandleMsg != null && tempHandleMsg.length > 50000) {
                            tempHandleMsg = tempHandleMsg.substring(0, 50000) + "..."
                        }
                        currentContext.handleMsg = tempHandleMsg
                    }
                    val finalContext = TaskPilotContext.getTaskPilotContext()
                    TaskPilotHelper.log(
                        "<br>----------- task-pilot 任务执行结束(成功) -----------<br>----------- 结果: 处理码=" +
                            finalContext?.handleCode +
                            ", 处理消息 = " +
                            finalContext?.handleMsg
                    )
                } else if (idleTimes > 30 && triggerQueue.isEmpty()) {
                    TaskPilotExecutor.removeTaskThread(taskId, "执行器空闲次数超过阈值。")
                }
            } catch (e: Throwable) {
                if (toStop) {
                    TaskPilotHelper.log("<br>----------- 任务线程已停止，停止原因:$stopReason")
                }

                val stringWriter = StringWriter()
                e.printStackTrace(PrintWriter(stringWriter))
                val errorMsg = stringWriter.toString()

                TaskPilotHelper.handleFail(errorMsg)
                TaskPilotHelper.log("<br>----------- 任务线程异常:$errorMsg<br>----------- task-pilot 任务执行结束(异常) -----------")
            } finally {
                if (triggerParam != null) {
                    val currentContext = TaskPilotContext.getTaskPilotContext()
                    if (!toStop) {
                        TriggerCallbackThread.pushCallBack(
                            CallbackRequest(
                                triggerParam.logId,
                                triggerParam.logDateTime,
                                currentContext?.handleCode ?: TaskPilotContext.HANDLE_CODE_FAIL,
                                currentContext?.handleMsg
                            )
                        )
                    } else {
                        TriggerCallbackThread.pushCallBack(
                            CallbackRequest(
                                triggerParam.logId,
                                triggerParam.logDateTime,
                                TaskPilotContext.HANDLE_CODE_FAIL,
                                "$stopReason [job running, killed]"
                            )
                        )
                    }
                }
            }
        }

        while (triggerQueue.isNotEmpty()) {
            val triggerParam = triggerQueue.poll() ?: continue
            TriggerCallbackThread.pushCallBack(
                CallbackRequest(
                    triggerParam.logId,
                    triggerParam.logDateTime,
                    TaskPilotContext.HANDLE_CODE_FAIL,
                    "$stopReason [job not executed, in the job queue, killed.]"
                )
            )
        }

        try {
            handler.destroy()
        } catch (e: Throwable) {
            logger.error(">>>>>>>>>>> 销毁任务处理器时发生异常。taskId={}", taskId, e)
        }

        logger.info(">>>>>>>>>>> task-pilot 任务线程已停止，thread:{}", currentThread())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskThread::class.java)
    }
}
