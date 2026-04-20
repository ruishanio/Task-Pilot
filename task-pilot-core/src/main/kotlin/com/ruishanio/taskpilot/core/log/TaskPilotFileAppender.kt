package com.ruishanio.taskpilot.core.log

import com.ruishanio.taskpilot.core.openapi.model.LogResult
import com.ruishanio.taskpilot.tool.core.DateTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.io.FileTool
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

/**
 * 任务日志文件读写工具。
 */
object TaskPilotFileAppender {
    private val logger = LoggerFactory.getLogger(TaskPilotFileAppender::class.java)

    private var logBasePath: String = defaultLogBasePath()
    private var glueSrcPath: String = logBasePath + File.separator + "gluesource"
    private var callbackLogPath: String = logBasePath + File.separator + "callbacklogs"

    /**
     * 默认日志目录落到用户主目录下，避免示例工程误写只读目录。
     */
    private fun defaultLogBasePath(): String = File(System.getProperty("user.home"), "logs/task-pilot/taskhandler").path

    /**
     * 初始化日志、脚本和失败回调目录。
     */
    @Throws(IOException::class)
    fun initLogPath(logPath: String?) {
        if (StringTool.isNotBlank(logPath)) {
            logBasePath = logPath!!.trim()
        }

        val logPathDir = File(logBasePath)
        FileTool.createDirectories(logPathDir)
        logBasePath = logPathDir.path

        val glueBaseDir = File(logPathDir, "gluesource")
        FileTool.createDirectories(glueBaseDir)
        glueSrcPath = glueBaseDir.path

        val callbackBaseDir = File(logPathDir, "callbacklogs")
        FileTool.createDirectories(callbackBaseDir)
        callbackLogPath = callbackBaseDir.path
    }
    fun getLogPath(): String = logBasePath
    fun getGlueSrcPath(): String = glueSrcPath
    fun getCallbackLogPath(): String = callbackLogPath

    /**
     * 生成任务日志文件名。
     */
    fun makeLogFileName(triggerDate: Date, logId: Long): String {
        val logFilePath = File(getLogPath(), DateTool.formatDate(triggerDate))
        try {
            FileTool.createDirectories(logFilePath)
        } catch (e: IOException) {
            throw RuntimeException("TaskPilotFileAppender makeLogFileName error, logFilePath:${logFilePath.path}", e)
        }

        return logFilePath.path + File.separator + logId + ".log"
    }

    /**
     * 追加一行日志。
     */
    fun appendLog(logFileName: String?, appendLog: String?) {
        if (StringTool.isBlank(logFileName) || appendLog == null) {
            return
        }
        val finalLogFileName = logFileName ?: return

        try {
            FileTool.writeLines(finalLogFileName, listOf(appendLog), true)
        } catch (e: IOException) {
            throw RuntimeException("TaskPilotFileAppender appendLog error, logFileName:$logFileName", e)
        }
    }

    /**
     * 读取任务日志。
     */
    fun readLog(logFileName: String?, fromLineNum: Int): LogResult {
        if (StringTool.isBlank(logFileName)) {
            return LogResult(fromLineNum, 0, "readLog fail, logFile not found", true)
        }
        val finalLogFileName = logFileName ?: return LogResult(fromLineNum, 0, "readLog fail, logFile not found", true)
        if (!FileTool.exists(finalLogFileName)) {
            return LogResult(fromLineNum, 0, "readLog fail, logFile not exists", true)
        }

        val logContentBuilder = StringBuilder()
        val toLineNum = AtomicInteger(0)
        val currentLineNum = AtomicInteger(0)

        try {
            FileTool.readLines(finalLogFileName) { line ->
                currentLineNum.incrementAndGet()
                if (currentLineNum.get() < fromLineNum) {
                    return@readLines
                }

                toLineNum.set(currentLineNum.get())
                logContentBuilder.append(line).append(System.lineSeparator())
            }
        } catch (e: IOException) {
            logger.error("读取任务日志文件时发生异常，logFileName={}, fromLineNum={}", logFileName, fromLineNum, e)
        }

        return LogResult(fromLineNum, toLineNum.get(), logContentBuilder.toString(), false)
    }
}
