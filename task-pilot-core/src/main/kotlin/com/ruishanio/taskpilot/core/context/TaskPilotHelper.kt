package com.ruishanio.taskpilot.core.context

import com.ruishanio.taskpilot.core.log.TaskPilotFileAppender
import com.ruishanio.taskpilot.tool.core.DateTool
import org.slf4j.LoggerFactory
import org.slf4j.helpers.MessageFormatter
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

/**
 * TaskPilot 执行辅助工具。
 *
 * 统一封装上下文读取、日志输出和执行结果写回，避免任务代码直接操作上下文细节。
 */
object TaskPilotHelper {
    private val logger = LoggerFactory.getLogger("task-pilot logger")

    /**
     * 获取当前任务 ID。
     */
    fun getJobId(): Long = TaskPilotContext.getTaskPilotContext()?.jobId ?: -1

    /**
     * 获取当前任务参数。
     */
    fun getJobParam(): String? = TaskPilotContext.getTaskPilotContext()?.jobParam

    /**
     * 获取当前日志 ID。
     */
    fun getLogId(): Long = TaskPilotContext.getTaskPilotContext()?.logId ?: -1

    /**
     * 获取当前日志时间戳。
     */
    fun getLogDateTime(): Long = TaskPilotContext.getTaskPilotContext()?.logDateTime ?: -1

    /**
     * 获取当前日志文件名。
     */
    fun getLogFileName(): String? = TaskPilotContext.getTaskPilotContext()?.logFileName

    /**
     * 获取当前分片序号。
     */
    fun getShardIndex(): Int = TaskPilotContext.getTaskPilotContext()?.shardIndex ?: -1

    /**
     * 获取当前分片总数。
     */
    fun getShardTotal(): Int = TaskPilotContext.getTaskPilotContext()?.shardTotal ?: -1

    /**
     * 追加格式化日志。
     */
    fun log(appendLogPattern: String?, vararg appendLogArguments: Any?): Boolean {
        val formattingTuple = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments)
        val appendLog = formattingTuple.message
        val callInfo = Throwable().stackTrace[1]
        return logDetail(callInfo, appendLog)
    }

    /**
     * 追加异常堆栈。
     */
    fun log(e: Throwable): Boolean {
        val stringWriter = StringWriter()
        e.printStackTrace(PrintWriter(stringWriter))
        val callInfo = Throwable().stackTrace[1]
        return logDetail(callInfo, stringWriter.toString())
    }

    /**
     * 统一写入任务日志，缺失日志文件时退化到普通 logger。
     */
    private fun logDetail(callInfo: StackTraceElement, appendLog: String?): Boolean {
        val taskPilotContext = TaskPilotContext.getTaskPilotContext() ?: return false
        val formatAppendLog =
            DateTool.formatDateTime(Date()) +
                " " +
                "[${callInfo.className}#${callInfo.methodName}]-" +
                "[${callInfo.lineNumber}]-" +
                "[${Thread.currentThread().name}] " +
                (appendLog ?: "")

        val logFileName = taskPilotContext.logFileName
        return if (!logFileName.isNullOrBlank()) {
            TaskPilotFileAppender.appendLog(logFileName, formatAppendLog)
            true
        } else {
            logger.info(">>>>>>>>>>> {}", formatAppendLog)
            false
        }
    }

    /**
     * 标记任务执行成功。
     */
    fun handleSuccess(): Boolean = handleResult(TaskPilotContext.HANDLE_CODE_SUCCESS, null)

    /**
     * 标记任务执行成功并写入摘要。
     */
    fun handleSuccess(handleMsg: String?): Boolean = handleResult(TaskPilotContext.HANDLE_CODE_SUCCESS, handleMsg)

    /**
     * 标记任务执行失败。
     */
    fun handleFail(): Boolean = handleResult(TaskPilotContext.HANDLE_CODE_FAIL, null)

    /**
     * 标记任务执行失败并写入摘要。
     */
    fun handleFail(handleMsg: String?): Boolean = handleResult(TaskPilotContext.HANDLE_CODE_FAIL, handleMsg)

    /**
     * 标记任务执行超时。
     */
    fun handleTimeout(): Boolean = handleResult(TaskPilotContext.HANDLE_CODE_TIMEOUT, null)

    /**
     * 标记任务执行超时并写入摘要。
     */
    fun handleTimeout(handleMsg: String?): Boolean = handleResult(TaskPilotContext.HANDLE_CODE_TIMEOUT, handleMsg)

    /**
     * 回写执行结果到上下文。
     */
    fun handleResult(handleCode: Int, handleMsg: String?): Boolean {
        val taskPilotContext = TaskPilotContext.getTaskPilotContext() ?: return false
        taskPilotContext.handleCode = handleCode
        if (handleMsg != null) {
            taskPilotContext.handleMsg = handleMsg
        }
        return true
    }
}
