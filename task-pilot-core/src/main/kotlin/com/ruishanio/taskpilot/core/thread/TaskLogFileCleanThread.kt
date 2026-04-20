package com.ruishanio.taskpilot.core.thread

import com.ruishanio.taskpilot.core.log.TaskPilotFileAppender
import com.ruishanio.taskpilot.tool.core.DateTool
import com.ruishanio.taskpilot.tool.io.FileTool
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * 任务日志清理线程。
 */
class TaskLogFileCleanThread private constructor() {
    private var localThread: Thread? = null

    @Volatile
    private var toStop: Boolean = false

    fun start(logRetentionDays: Long) {
        if (logRetentionDays < 3) {
            return
        }

        localThread =
            Thread {
                while (!toStop) {
                    try {
                        val childDirs = File(TaskPilotFileAppender.getLogPath()).listFiles()
                        if (!childDirs.isNullOrEmpty()) {
                            val todayCal =
                                Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                            val todayDate = todayCal.time

                            for (childFile in childDirs) {
                                if (!childFile.isDirectory) {
                                    continue
                                }
                                if (!childFile.name.contains("-")) {
                                    continue
                                }

                                val logFileCreateDate: Date? =
                                    try {
                                        DateTool.parseDate(childFile.name)
                                    } catch (e: Exception) {
                                        logger.error("解析日志目录日期时发生异常，directory={}", childFile.path, e)
                                        null
                                    }
                                if (logFileCreateDate == null) {
                                    continue
                                }

                                val expiredDate = DateTool.addDays(logFileCreateDate, logRetentionDays)
                                if (todayDate.time > expiredDate.time) {
                                    FileTool.delete(childFile)
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        if (!toStop) {
                            logger.error("清理过期任务日志目录时发生异常。", e)
                        }
                    }

                    try {
                        TimeUnit.DAYS.sleep(1)
                    } catch (e: Throwable) {
                        if (!toStop) {
                            logger.error("任务日志清理线程休眠时发生异常。", e)
                        }
                    }
                }
                logger.info(">>>>>>>>>>> task-pilot 执行器任务日志清理线程已销毁。")
            }.apply {
                isDaemon = true
                name = "task-pilot, executor TaskLogFileCleanThread"
                start()
            }
    }

    fun toStop() {
        toStop = true
        val thread = localThread ?: return
        thread.interrupt()
        try {
            thread.join()
        } catch (e: InterruptedException) {
            logger.error("停止任务日志清理线程时发生异常。", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskLogFileCleanThread::class.java)
        private val instance = TaskLogFileCleanThread()
        fun getInstance(): TaskLogFileCleanThread = instance
    }
}
