package com.ruishanio.taskpilot.admin.scheduler.thread

import com.ruishanio.taskpilot.admin.constant.TriggerStatus
import com.ruishanio.taskpilot.admin.model.TaskInfo
import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.misfire.toMisfireHandler
import com.ruishanio.taskpilot.admin.scheduler.trigger.TriggerTypeEnum
import com.ruishanio.taskpilot.admin.scheduler.type.toScheduleType
import com.ruishanio.taskpilot.core.enums.MisfireStrategyEnum
import com.ruishanio.taskpilot.core.enums.ScheduleTypeEnum
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.MapTool
import org.slf4j.LoggerFactory
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.DefaultTransactionDefinition
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 调度线程与时间轮辅助类。
 *
 * 主线程负责预读和刷新下次触发时间，时间轮只负责秒级触发，保持旧版两段式调度模型不变。
 */
class TaskScheduleHelper {
    private lateinit var scheduleThread: Thread
    private lateinit var ringThread: Thread

    @Volatile
    private var scheduleThreadToStop: Boolean = false

    @Volatile
    private var ringThreadToStop: Boolean = false

    private val ringData: MutableMap<Int, MutableList<Int>> = ConcurrentHashMap()

    fun start() {
        scheduleThread = Thread {
            try {
                TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000)
            } catch (e: Throwable) {
                if (!scheduleThreadToStop) {
                    logger.error("调度线程对齐时间时发生异常。", e)
                }
            }
            logger.info(">>>>>>>>> task-pilot 管理端调度线程初始化完成。")

            val preReadCount = (TaskPilotAdminBootstrap.instance.triggerPoolFastMax +
                TaskPilotAdminBootstrap.instance.triggerPoolSlowMax) * 10

            while (!scheduleThreadToStop) {
                val start = System.currentTimeMillis()
                var preReadSuc = true
                var transactionStatus: TransactionStatus? = null
                try {
                    transactionStatus = TaskPilotAdminBootstrap.instance.transactionManager
                        .getTransaction(DefaultTransactionDefinition())

                    // 通过查询锁表记录拿到数据库级锁，避免多实例重复调度。
                    TaskPilotAdminBootstrap.instance.taskLockMapper.scheduleLock()
                    val nowTime = System.currentTimeMillis()
                    val scheduleList = TaskPilotAdminBootstrap.instance.taskInfoMapper.scheduleTaskQuery(
                        nowTime + PRE_READ_MS,
                        preReadCount
                    )

                    if (CollectionTool.isNotEmpty(scheduleList)) {
                        for (taskInfo in scheduleList) {
                            if (nowTime > taskInfo.triggerNextTime + PRE_READ_MS) {
                                val misfireStrategyEnum = taskInfo.misfireStrategy ?: MisfireStrategyEnum.DO_NOTHING
                                misfireStrategyEnum.toMisfireHandler().handle(taskInfo.id)
                                refreshNextTriggerTime(taskInfo, Date())
                            } else if (nowTime > taskInfo.triggerNextTime) {
                                TaskPilotAdminBootstrap.instance.taskTriggerPoolHelper.trigger(
                                    taskInfo.id,
                                    TriggerTypeEnum.CRON,
                                    -1,
                                    null,
                                    null,
                                    null
                                )
                                logger.debug(">>>>>>>>>>> task-pilot 调度过期，直接触发任务，taskId={}", taskInfo.id)
                                refreshNextTriggerTime(taskInfo, Date())

                                if (taskInfo.triggerStatus == TriggerStatus.RUNNING.value &&
                                    nowTime + PRE_READ_MS > taskInfo.triggerNextTime
                                ) {
                                    val ringSecond = ((taskInfo.triggerNextTime / 1000) % 60).toInt()
                                    pushTimeRing(ringSecond, taskInfo.id)
                                    logger.debug(">>>>>>>>>>> task-pilot 调度预读命中，推入时间轮，taskId={}", taskInfo.id)
                                    refreshNextTriggerTime(taskInfo, Date(taskInfo.triggerNextTime))
                                }
                            } else {
                                val ringSecond = ((taskInfo.triggerNextTime / 1000) % 60).toInt()
                                pushTimeRing(ringSecond, taskInfo.id)
                                logger.debug(">>>>>>>>>>> task-pilot 常规调度推入时间轮，taskId={}", taskInfo.id)
                                refreshNextTriggerTime(taskInfo, Date(taskInfo.triggerNextTime))
                            }
                        }

                        for (taskInfo in scheduleList) {
                            TaskPilotAdminBootstrap.instance.taskInfoMapper.scheduleUpdate(taskInfo)
                        }
                    } else {
                        preReadSuc = false
                    }
                } catch (e: Throwable) {
                    if (!scheduleThreadToStop) {
                        logger.error(">>>>>>>>>>> task-pilot 调度线程执行时发生异常。", e)
                    }
                } finally {
                    try {
                        if (transactionStatus != null) {
                            TaskPilotAdminBootstrap.instance.transactionManager.commit(transactionStatus)
                        }
                    } catch (e: Throwable) {
                        logger.error(">>>>>>>>>>> task-pilot 调度线程提交事务时发生异常。", e)
                    }
                }

                val cost = System.currentTimeMillis() - start
                if (cost < 1000) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(
                            (if (preReadSuc) 1000 else PRE_READ_MS) - System.currentTimeMillis() % 1000
                        )
                    } catch (e: Throwable) {
                        if (!scheduleThreadToStop) {
                            logger.error("调度线程休眠等待下一轮扫描时发生异常。", e)
                        }
                    }
                }
            }
            logger.info(">>>>>>>>>>> task-pilot 调度线程已停止。")
        }
        scheduleThread.isDaemon = true
        scheduleThread.name = "task-pilot, admin TaskScheduleHelper#scheduleThread"
        scheduleThread.start()

        ringThread = Thread {
            while (!ringThreadToStop) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000)
                } catch (e: Throwable) {
                    if (!ringThreadToStop) {
                        logger.error("时间轮线程对齐秒级刻度时发生异常。", e)
                    }
                }

                try {
                    val ringItemData = ArrayList<Int>()
                    val nowSecond = Calendar.getInstance().get(Calendar.SECOND)
                    for (i in 0..2) {
                        val ringItemList = ringData.remove((nowSecond + 60 - i) % 60)
                        if (CollectionTool.isNotEmpty(ringItemList)) {
                            val ringItemListDistinct = ringItemList!!.distinct()
                            if (ringItemListDistinct.size < ringItemList.size) {
                                logger.warn(">>>>>>>>>>> task-pilot 时间轮检测到重复刻度任务，second={}, tasks={}", nowSecond, ringItemData)
                            }
                            ringItemData.addAll(ringItemListDistinct)
                        }
                    }

                    logger.debug(">>>>>>>>>>> task-pilot 时间轮触发刻度，second={}, tasks={}", nowSecond, ringItemData)
                    if (CollectionTool.isNotEmpty(ringItemData)) {
                        for (taskId in ringItemData) {
                            TaskPilotAdminBootstrap.instance.taskTriggerPoolHelper.trigger(
                                taskId,
                                TriggerTypeEnum.CRON,
                                -1,
                                null,
                                null,
                                null
                            )
                        }
                        ringItemData.clear()
                    }
                } catch (e: Throwable) {
                    if (!ringThreadToStop) {
                        logger.error(">>>>>>>>>>> task-pilot 时间轮线程执行时发生异常。", e)
                    }
                }
            }
            logger.info(">>>>>>>>>>> task-pilot 时间轮线程已停止。")
        }
        ringThread.isDaemon = true
        ringThread.name = "task-pilot, admin TaskScheduleHelper#ringThread"
        ringThread.start()
    }

    /**
     * 生成下次触发时间失败时直接停掉任务，防止非法配置反复进入调度循环。
     */
    private fun refreshNextTriggerTime(taskInfo: TaskInfo, fromTime: Date) {
        try {
            val scheduleTypeEnum = taskInfo.scheduleType ?: ScheduleTypeEnum.NONE
            val nextTriggerTime = scheduleTypeEnum.toScheduleType().generateNextTriggerTime(taskInfo, fromTime)

            if (nextTriggerTime != null) {
                taskInfo.triggerStatus = -1
                taskInfo.triggerLastTime = taskInfo.triggerNextTime
                taskInfo.triggerNextTime = nextTriggerTime.time
            } else {
                taskInfo.triggerStatus = TriggerStatus.STOPPED.value
                taskInfo.triggerLastTime = 0
                taskInfo.triggerNextTime = 0
                logger.error(
                    ">>>>>>>>>>> task-pilot 刷新下次触发时间失败，任务已停止。taskId={}, scheduleType={}, scheduleConf={}",
                    taskInfo.id,
                    taskInfo.scheduleType,
                    taskInfo.scheduleConf
                )
            }
        } catch (e: Throwable) {
            taskInfo.triggerStatus = TriggerStatus.STOPPED.value
            taskInfo.triggerLastTime = 0
            taskInfo.triggerNextTime = 0
            logger.error(
                ">>>>>>>>>>> task-pilot 刷新下次触发时间时发生异常，任务已停止。taskId={}, scheduleType={}, scheduleConf={}",
                taskInfo.id,
                taskInfo.scheduleType,
                taskInfo.scheduleConf,
                e
            )
        }
    }

    /**
     * 时间轮内只存任务 ID，真正执行时再交给触发线程池处理，避免时间轮本身阻塞。
     */
    private fun pushTimeRing(ringSecond: Int, taskId: Int) {
        val ringItemList = ringData.computeIfAbsent(ringSecond) { ArrayList() }
        ringItemList.add(taskId)
        logger.debug(">>>>>>>>>>> task-pilot 推入时间轮，second={}, tasks={}", ringSecond, ringItemList)
    }

    fun stop() {
        scheduleThreadToStop = true
        try {
            TimeUnit.SECONDS.sleep(1)
        } catch (e: Throwable) {
            logger.error("停止调度线程前等待缓冲时间时发生异常。", e)
        }
        if (scheduleThread.state != Thread.State.TERMINATED) {
            scheduleThread.interrupt()
            try {
                scheduleThread.join()
            } catch (e: Throwable) {
                logger.error("停止调度线程并等待结束时发生异常。", e)
            }
        }

        var hasRingData = false
        if (MapTool.isNotEmpty(ringData)) {
            for (second in ringData.keys) {
                val ringItemList = ringData[second]
                if (CollectionTool.isNotEmpty(ringItemList)) {
                    hasRingData = true
                    break
                }
            }
        }
        if (hasRingData) {
            try {
                TimeUnit.SECONDS.sleep(ELEGANT_SHUTDOWN_WAITING_SECONDS)
            } catch (e: Throwable) {
                logger.error("优雅关闭时间轮前等待剩余任务时发生异常。", e)
            }
        }

        ringThreadToStop = true
        try {
            TimeUnit.SECONDS.sleep(1)
        } catch (e: Throwable) {
            logger.error("停止时间轮线程前等待缓冲时间时发生异常。", e)
        }
        if (ringThread.state != Thread.State.TERMINATED) {
            ringThread.interrupt()
            try {
                ringThread.join()
            } catch (e: Throwable) {
                logger.error("停止时间轮线程并等待结束时发生异常。", e)
            }
        }

        logger.info(">>>>>>>>>>> task-pilot 调度辅助线程已停止。")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskScheduleHelper::class.java)

        /**
         * 调度线程预读窗口，控制任务何时进入时间轮。
         */
        const val PRE_READ_MS: Long = 5000

        /**
         * 优雅停机时等待时间轮剩余任务消费的最大时长。
         */
        private const val ELEGANT_SHUTDOWN_WAITING_SECONDS: Long = 10
    }
}
