package com.ruishanio.taskpilot.admin.scheduler.thread

import com.ruishanio.taskpilot.admin.model.TaskReport
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import org.slf4j.LoggerFactory
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * 日志报表刷新与过期日志清理线程。
 *
 * 报表只回刷最近 3 天，兼顾首页统计准确性和全表扫描成本。
 */
class JobLogReportHelper {
    private lateinit var logReportThread: Thread

    @Volatile
    private var toStop: Boolean = false

    fun start() {
        logReportThread = Thread {
            var lastCleanLogTime = 0L
            while (!toStop) {
                try {
                    for (i in 0 until 3) {
                        val itemDay = Calendar.getInstance()
                        itemDay.add(Calendar.DAY_OF_MONTH, -i)
                        itemDay.set(Calendar.HOUR_OF_DAY, 0)
                        itemDay.set(Calendar.MINUTE, 0)
                        itemDay.set(Calendar.SECOND, 0)
                        itemDay.set(Calendar.MILLISECOND, 0)
                        val todayFrom = itemDay.time

                        itemDay.set(Calendar.HOUR_OF_DAY, 23)
                        itemDay.set(Calendar.MINUTE, 59)
                        itemDay.set(Calendar.SECOND, 59)
                        itemDay.set(Calendar.MILLISECOND, 999)
                        val todayTo = itemDay.time

                        val taskReport = TaskReport().apply {
                            triggerDay = todayFrom
                            runningCount = 0
                            sucCount = 0
                            failCount = 0
                        }

                        val triggerCountMap = TaskPilotAdminBootstrap.instance.taskLogMapper.findLogReport(todayFrom, todayTo)
                        if (!triggerCountMap.isNullOrEmpty()) {
                            val triggerDayCount = (triggerCountMap["triggerDayCount"]?.toString()?.toInt() ?: 0)
                            val triggerDayCountRunning = (triggerCountMap["triggerDayCountRunning"]?.toString()?.toInt() ?: 0)
                            val triggerDayCountSuc = (triggerCountMap["triggerDayCountSuc"]?.toString()?.toInt() ?: 0)
                            val triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc

                            taskReport.runningCount = triggerDayCountRunning
                            taskReport.sucCount = triggerDayCountSuc
                            taskReport.failCount = triggerDayCountFail
                        }

                        TaskPilotAdminBootstrap.instance.taskReportMapper.saveOrUpdate(taskReport)
                    }
                } catch (e: Throwable) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> task-pilot 刷新任务日志报表时发生异常。", e)
                    }
                }

                try {
                    if (TaskPilotAdminBootstrap.instance.logretentiondays > 0 &&
                        System.currentTimeMillis() - lastCleanLogTime > 24 * 60 * 60 * 1000
                    ) {
                        val expiredDay = Calendar.getInstance()
                        expiredDay.add(Calendar.DAY_OF_MONTH, -1 * TaskPilotAdminBootstrap.instance.logretentiondays)
                        expiredDay.set(Calendar.HOUR_OF_DAY, 0)
                        expiredDay.set(Calendar.MINUTE, 0)
                        expiredDay.set(Calendar.SECOND, 0)
                        expiredDay.set(Calendar.MILLISECOND, 0)
                        val clearBeforeTime = expiredDay.time

                        var logIds: List<Long>
                        do {
                            logIds = TaskPilotAdminBootstrap.instance.taskLogMapper.findClearLogIds(
                                0,
                                0,
                                clearBeforeTime,
                                0,
                                1000
                            )
                            if (logIds.isNotEmpty()) {
                                TaskPilotAdminBootstrap.instance.taskLogMapper.clearLog(logIds)
                            }
                        } while (logIds.isNotEmpty())

                        lastCleanLogTime = System.currentTimeMillis()
                    }
                } catch (e: Throwable) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> task-pilot 清理过期任务日志时发生异常。", e)
                    }
                }

                try {
                    TimeUnit.MINUTES.sleep(1)
                } catch (e: Throwable) {
                    if (!toStop) {
                        logger.error("任务日志报表线程休眠时发生异常。", e)
                    }
                }
            }
            logger.info(">>>>>>>>>>> task-pilot 任务日志报表线程已停止。")
        }
        logReportThread.isDaemon = true
        logReportThread.name = "task-pilot, admin JobLogReportHelper"
        logReportThread.start()
    }

    fun stop() {
        toStop = true
        logReportThread.interrupt()
        try {
            logReportThread.join()
        } catch (e: Throwable) {
            logger.error("停止任务日志报表线程时发生异常。", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobLogReportHelper::class.java)
    }
}
