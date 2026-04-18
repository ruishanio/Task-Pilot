package com.ruishanio.taskpilot.core.thread

import com.ruishanio.taskpilot.core.constant.Const
import com.ruishanio.taskpilot.core.context.TaskPilotContext
import com.ruishanio.taskpilot.core.context.TaskPilotHelper
import com.ruishanio.taskpilot.core.executor.TaskPilotExecutor
import com.ruishanio.taskpilot.core.log.TaskPilotFileAppender
import com.ruishanio.taskpilot.core.openapi.model.CallbackRequest
import com.ruishanio.taskpilot.tool.core.ArrayTool
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.crypto.Md5Tool
import com.ruishanio.taskpilot.tool.io.FileTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Date
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * 任务回调线程。
 *
 * 负责把执行结果回传给调度中心，回传失败时落文件并由重试线程兜底。
 */
class TriggerCallbackThread private constructor() {
    private val callBackQueue = LinkedBlockingQueue<CallbackRequest>()
    private var triggerCallbackThread: Thread? = null
    private var triggerRetryCallbackThread: Thread? = null

    @Volatile
    private var toStop: Boolean = false

    fun start() {
        if (TaskPilotExecutor.getAdminBizList() == null) {
            logger.warn(">>>>>>>>>>> task-pilot，执行器回调配置无效，adminAddresses 为空。")
            return
        }

        triggerCallbackThread =
            Thread {
                while (!toStop) {
                    try {
                        val callback = callBackQueue.take()
                        val callbackParamList = ArrayList<CallbackRequest>()
                        callbackParamList.add(callback)
                        callBackQueue.drainTo(callbackParamList)
                        if (CollectionTool.isNotEmpty(callbackParamList)) {
                            doCallback(callbackParamList)
                        }
                    } catch (e: Throwable) {
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> task-pilot 执行器回调线程处理回调队列时发生异常。", e)
                        }
                    }
                }

                try {
                    val callbackParamList = ArrayList<CallbackRequest>()
                    callBackQueue.drainTo(callbackParamList)
                    if (CollectionTool.isNotEmpty(callbackParamList)) {
                        doCallback(callbackParamList)
                    }
                } catch (e: Throwable) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> task-pilot 执行器回调线程停止前处理剩余回调时发生异常。", e)
                    }
                }
                logger.info(">>>>>>>>>>> task-pilot，执行器回调线程已销毁。")
            }.apply {
                isDaemon = true
                name = "task-pilot, executor TriggerCallbackThread"
                start()
            }

        triggerRetryCallbackThread =
            Thread {
                while (!toStop) {
                    try {
                        retryFailCallbackFile()
                    } catch (e: Throwable) {
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> task-pilot 执行器失败回调重试时发生异常。", e)
                        }
                    }
                    try {
                        TimeUnit.SECONDS.sleep(Const.BEAT_TIMEOUT.toLong())
                    } catch (e: Throwable) {
                        if (!toStop) {
                            logger.error(">>>>>>>>>>> task-pilot 执行器失败回调重试线程休眠时发生异常。", e)
                        }
                    }
                }
                logger.info(">>>>>>>>>>> task-pilot，执行器失败回调重试线程已销毁。")
            }.apply {
                isDaemon = true
                name = "task-pilot, executor TriggerRetryCallbackThread"
                start()
            }
    }

    fun toStop() {
        toStop = true

        triggerCallbackThread?.let { thread ->
            thread.interrupt()
            try {
                thread.join()
            } catch (e: Throwable) {
                logger.error(">>>>>>>>>>> task-pilot 停止执行器回调线程时发生异常。", e)
            }
        }

        triggerRetryCallbackThread?.let { thread ->
            thread.interrupt()
            try {
                thread.join()
            } catch (e: Throwable) {
                logger.error(">>>>>>>>>>> task-pilot 停止执行器失败回调重试线程时发生异常。", e)
            }
        }
    }

    /**
     * 执行一次回调，失败时落盘以便后续重试。
     */
    private fun doCallback(callbackParamList: List<CallbackRequest>) {
        var callbackRet = false
        for (adminBiz in TaskPilotExecutor.getAdminBizList().orEmpty()) {
            try {
                val callbackResult = adminBiz.callback(callbackParamList)
                if (callbackResult.isSuccess) {
                    callbackLog(callbackParamList, "<br>----------- task-pilot 任务回调完成。")
                    callbackRet = true
                    break
                } else {
                    callbackLog(callbackParamList, "<br>----------- task-pilot 任务回调失败，回调结果:$callbackResult")
                }
            } catch (e: Throwable) {
                callbackLog(callbackParamList, "<br>----------- task-pilot 任务回调异常，异常信息:${e.message}")
            }
        }
        if (!callbackRet) {
            appendFailCallbackFile(callbackParamList)
        }
    }

    /**
     * 把回调结果附加到对应任务日志中。
     */
    private fun callbackLog(callbackParamList: List<CallbackRequest>, logContent: String) {
        for (callbackParam in callbackParamList) {
            val logFileName =
                TaskPilotFileAppender.makeLogFileName(Date(callbackParam.logDateTim), callbackParam.logId)
            TaskPilotContext.setTaskPilotContext(
                TaskPilotContext(
                    -1,
                    null,
                    -1,
                    -1,
                    logFileName,
                    -1,
                    -1
                )
            )
            TaskPilotHelper.log(logContent)
        }
    }

    /**
     * 追加失败回调文件。
     */
    private fun appendFailCallbackFile(callbackParamList: List<CallbackRequest>) {
        if (CollectionTool.isEmpty(callbackParamList)) {
            return
        }

        val callbackData = GsonTool.toJson(callbackParamList)
        val callbackDataMd5 = Md5Tool.md5(callbackData)
        val finalLogFileName = buildFailCallbackFileName(callbackDataMd5)

        try {
            FileTool.writeString(finalLogFileName, callbackData)
        } catch (e: IOException) {
            logger.error(">>>>>>>>>>> TriggerCallbackThread 追加失败回调文件时发生异常，finalLogFileName:{}", finalLogFileName, e)
        }
    }

    /**
     * 扫描失败回调文件并重试。
     */
    private fun retryFailCallbackFile() {
        val callbackLogPath = File(TaskPilotFileAppender.getCallbackLogPath())
        if (!callbackLogPath.exists()) {
            return
        }
        if (!FileTool.isDirectory(callbackLogPath)) {
            FileTool.delete(callbackLogPath)
            return
        }
        if (ArrayTool.isEmpty(callbackLogPath.listFiles())) {
            return
        }

        for (callbackLogFile in callbackLogPath.listFiles().orEmpty()) {
            try {
                val callbackData = FileTool.readString(callbackLogFile.path)
                if (StringTool.isBlank(callbackData)) {
                    FileTool.delete(callbackLogFile)
                    continue
                }

                val callbackParamList = GsonTool.fromJsonList(callbackData, CallbackRequest::class.java)
                FileTool.delete(callbackLogFile)
                doCallback(callbackParamList)
            } catch (e: IOException) {
                logger.error(">>>>>>>>>>> TriggerCallbackThread 重试失败回调文件时发生异常，callbackLogFile:{}", callbackLogFile.path, e)
            }
        }
    }

    private fun buildFailCallbackFileName(callbackDataMd5: String): String =
        TaskPilotFileAppender.getCallbackLogPath() +
            File.separator +
            "task-pilot-callback-$callbackDataMd5.log"

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerCallbackThread::class.java)
        private val instance = TriggerCallbackThread()
        fun getInstance(): TriggerCallbackThread = instance

        /**
         * 推送回调请求到队列。
         */
        fun pushCallBack(callback: CallbackRequest) {
            instance.callBackQueue.add(callback)
            logger.debug(">>>>>>>>>>> task-pilot，推送回调请求，logId:{}", callback.logId)
        }
    }
}
