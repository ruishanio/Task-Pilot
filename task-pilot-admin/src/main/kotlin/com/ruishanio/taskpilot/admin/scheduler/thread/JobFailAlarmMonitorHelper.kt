package com.ruishanio.taskpilot.admin.scheduler.thread

import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.trigger.TriggerTypeEnum
import com.ruishanio.taskpilot.admin.util.I18nUtil
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * 失败任务告警监控线程。
 *
 * 先通过 alarmStatus 做乐观锁占位，再执行重试和告警，避免多实例重复处理同一条失败日志。
 */
class JobFailAlarmMonitorHelper {
    private lateinit var monitorThread: Thread

    @Volatile
    private var toStop: Boolean = false

    fun start() {
        monitorThread = Thread {
            while (!toStop) {
                try {
                    val failLogIds = TaskPilotAdminBootstrap.instance.taskPilotLogMapper.findFailJobLogIds(1000)
                    if (failLogIds.isNotEmpty()) {
                        for (failLogId in failLogIds) {
                            val lockRet = TaskPilotAdminBootstrap.instance.taskPilotLogMapper.updateAlarmStatus(failLogId, 0, -1)
                            if (lockRet < 1) {
                                continue
                            }

                            val log = TaskPilotAdminBootstrap.instance.taskPilotLogMapper.load(failLogId) ?: continue
                            val info = TaskPilotAdminBootstrap.instance.taskPilotInfoMapper.loadById(log.jobId)

                            if (log.executorFailRetryCount > 0) {
                                TaskPilotAdminBootstrap.instance.jobTriggerPoolHelper.trigger(
                                    log.jobId,
                                    TriggerTypeEnum.RETRY,
                                    log.executorFailRetryCount - 1,
                                    log.executorShardingParam,
                                    log.executorParam,
                                    null
                                )
                                val retryMsg =
                                    "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>${I18nUtil.getString("jobconf_trigger_type_retry")}<<<<<<<<<<< </span><br>"
                                log.triggerMsg = log.triggerMsg + retryMsg
                                TaskPilotAdminBootstrap.instance.taskPilotLogMapper.updateTriggerInfo(log)
                            }

                            val newAlarmStatus = if (info != null) {
                                val alarmResult = TaskPilotAdminBootstrap.instance.jobAlarmer.alarm(info, log)
                                if (alarmResult) 2 else 3
                            } else {
                                1
                            }
                            TaskPilotAdminBootstrap.instance.taskPilotLogMapper.updateAlarmStatus(failLogId, -1, newAlarmStatus)
                        }
                    }
                } catch (e: Throwable) {
                    if (!toStop) {
                        logger.error(">>>>>>>>>>> task-pilot 失败任务告警线程执行时发生异常。", e)
                    }
                }

                try {
                    TimeUnit.SECONDS.sleep(10)
                } catch (e: Throwable) {
                    if (!toStop) {
                        logger.error("失败任务告警线程休眠时发生异常。", e)
                    }
                }
            }
            logger.info(">>>>>>>>>>> task-pilot 失败任务告警线程已停止。")
        }
        monitorThread.isDaemon = true
        monitorThread.name = "task-pilot, admin JobFailMonitorHelper"
        monitorThread.start()
    }

    fun stop() {
        toStop = true
        monitorThread.interrupt()
        try {
            monitorThread.join()
        } catch (e: Throwable) {
            logger.error("停止失败任务告警线程时发生异常。", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobFailAlarmMonitorHelper::class.java)
    }
}
