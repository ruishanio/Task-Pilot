package com.ruishanio.taskpilot.admin.scheduler.thread

import com.ruishanio.taskpilot.admin.model.TaskLog
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.core.openapi.model.CallbackRequest
import com.ruishanio.taskpilot.tool.core.DateTool
import com.ruishanio.taskpilot.tool.response.Response
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 回调与结果丢失补偿辅助类。
 *
 * 结果回调和“运行中超时未回调”补偿共用同一完成处理器，确保子任务触发和日志收尾逻辑一致。
 */
class JobCompleteHelper {
    private lateinit var callbackThreadPool: ThreadPoolExecutor
    private lateinit var monitorThread: Thread

    @Volatile
    private var toStop: Boolean = false

    fun start() {
        callbackThreadPool = ThreadPoolExecutor(
            2,
            20,
            30L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(3000),
            ThreadFactory { runnable ->
                Thread(runnable, "task-pilot, admin JobLosedMonitorHelper-callbackThreadPool-${runnable.hashCode()}")
            },
            RejectedExecutionHandler { runnable, _ ->
                runnable.run()
                logger.warn(">>>>>>>>>>> task-pilot 回调处理过于频繁，线程池已触发拒绝策略，改为调用线程直接执行。")
            }
        )

        monitorThread = Thread {
            try {
                TimeUnit.MILLISECONDS.sleep(50)
            } catch (e: Throwable) {
                if (!toStop) {
                    logger.error("任务结果补偿线程初始化等待时发生异常。", e)
                }
            }

            while (!toStop) {
                try {
                    val losedTime = DateTool.addMinutes(Date(), -10)
                    val losedJobIds = TaskPilotAdminBootstrap.instance.taskLogMapper.findLostJobIds(losedTime)
                    if (losedJobIds.isNotEmpty()) {
                        for (logId in losedJobIds) {
                            val jobLog = TaskLog().apply {
                                id = logId
                                handleTime = Date()
                                handleCode = TaskPilotContext.HANDLE_CODE_FAIL
                                handleMsg = "任务结果丢失，标记失败"
                            }
                            TaskPilotAdminBootstrap.instance.jobCompleter.complete(jobLog)
                        }
                    }
                } catch (e: Throwable) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> task-pilot 任务结果补偿线程执行时发生异常。", e)
                    }
                }

                try {
                    TimeUnit.SECONDS.sleep(60)
                } catch (e: Throwable) {
                    if (!toStop) {
                        logger.error("任务结果补偿线程休眠时发生异常。", e)
                    }
                }
            }

            logger.info(">>>>>>>>>>> task-pilot 任务结果补偿线程已停止。")
        }
        monitorThread.isDaemon = true
        monitorThread.name = "task-pilot, admin JobLosedMonitorHelper"
        monitorThread.start()
    }

    fun stop() {
        toStop = true
        callbackThreadPool.shutdownNow()
        monitorThread.interrupt()
        try {
            monitorThread.join()
        } catch (e: Throwable) {
            logger.error("停止任务结果补偿线程时发生异常。", e)
        }
    }

    /**
     * 回调批量异步处理，避免执行器回调线程被管理端数据库写入拖慢。
     */
    fun callback(callbackParamList: List<CallbackRequest>): Response<String> {
        callbackThreadPool.execute {
            for (callbackRequest in callbackParamList) {
                val callbackResult = doCallback(callbackRequest)
                logger.debug(
                    ">>>>>>>>> 回调处理{}，callbackRequest={}, callbackResult={}",
                    if (callbackResult.isSuccess) "成功" else "失败",
                    callbackRequest,
                    callbackResult
                )
            }
        }
        return Response.ofSuccess()
    }

    /**
     * 回调只允许成功写入一次，避免重复回调重复触发子任务。
     */
    private fun doCallback(handleCallbackParam: CallbackRequest): Response<String> {
        val log = TaskPilotAdminBootstrap.instance.taskLogMapper.load(handleCallbackParam.logId)
            ?: return Response.ofFail("未找到对应的日志记录。")
        if (log.handleCode > 0) {
            return Response.ofFail("日志回调重复。")
        }

        val handleMsg = StringBuilder()
        if (log.handleMsg != null) {
            handleMsg.append(log.handleMsg).append("<br>")
        }
        if (handleCallbackParam.handleMsg != null) {
            handleMsg.append(handleCallbackParam.handleMsg)
        }

        log.handleTime = Date()
        log.handleCode = handleCallbackParam.handleCode
        log.handleMsg = handleMsg.toString()
        TaskPilotAdminBootstrap.instance.jobCompleter.complete(log)
        return Response.ofSuccess()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobCompleteHelper::class.java)
    }
}
