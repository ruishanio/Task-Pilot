package com.ruishanio.taskpilot.admin.scheduler.thread

import com.ruishanio.taskpilot.admin.scheduler.config.TaskPilotAdminBootstrap
import com.ruishanio.taskpilot.admin.scheduler.trigger.TriggerTypeEnum
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * 触发线程池辅助类。
 *
 * 仍然保留快慢双线程池与按分钟滚动的超时统计，避免单个慢任务持续挤占普通触发流量。
 */
class TaskTriggerPoolHelper {
    private lateinit var fastTriggerPool: ThreadPoolExecutor
    private lateinit var slowTriggerPool: ThreadPoolExecutor

    @Volatile
    private var minTim: Long = System.currentTimeMillis() / 60000

    @Volatile
    private var taskTimeoutCountMap: ConcurrentMap<Int, AtomicInteger> = ConcurrentHashMap()

    fun start() {
        fastTriggerPool = ThreadPoolExecutor(
            10,
            TaskPilotAdminBootstrap.instance.triggerPoolFastMax,
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(2000),
            ThreadFactory { runnable ->
                Thread(runnable, "task-pilot, admin TaskTriggerPoolHelper-fastTriggerPool-${runnable.hashCode()}")
            },
            RejectedExecutionHandler { runnable, _ ->
                logger.error(">>>>>>>>>>> task-pilot 快速触发线程池执行过于频繁，任务被拒绝。runnable={}", runnable)
            }
        )

        slowTriggerPool = ThreadPoolExecutor(
            10,
            TaskPilotAdminBootstrap.instance.triggerPoolSlowMax,
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(5000),
            ThreadFactory { runnable ->
                Thread(runnable, "task-pilot, admin TaskTriggerPoolHelper-slowTriggerPool-${runnable.hashCode()}")
            },
            RejectedExecutionHandler { runnable, _ ->
                logger.error(">>>>>>>>>>> task-pilot 慢速触发线程池执行过于频繁，任务被拒绝。runnable={}", runnable)
            }
        )
    }

    fun stop() {
        fastTriggerPool.shutdownNow()
        slowTriggerPool.shutdownNow()
        logger.info(">>>>>>>>> task-pilot 触发线程池已关闭。")
    }

    /**
     * 同一任务在 1 分钟内超时过多时切换到慢线程池，降低对普通任务的干扰。
     */
    fun trigger(
        taskId: Int,
        triggerType: TriggerTypeEnum,
        failRetryCount: Int,
        executorShardingParam: String?,
        executorParam: String?,
        addressList: String?
    ) {
        var triggerPool = fastTriggerPool
        val taskTimeoutCount = taskTimeoutCountMap[taskId]
        if (taskTimeoutCount != null && taskTimeoutCount.get() > 10) {
            triggerPool = slowTriggerPool
        }

        triggerPool.execute(object : Runnable {
            override fun run() {
                val start = System.currentTimeMillis()
                try {
                    TaskPilotAdminBootstrap.instance.taskTrigger.trigger(
                        taskId,
                        triggerType,
                        failRetryCount,
                        executorShardingParam,
                        executorParam,
                        addressList
                    )
                } catch (e: Throwable) {
                    logger.error("触发任务时发生异常。taskId={}, triggerType={}", taskId, triggerType, e)
                } finally {
                    val minTimNow = System.currentTimeMillis() / 60000
                    if (minTim != minTimNow) {
                        minTim = minTimNow
                        taskTimeoutCountMap.clear()
                    }

                    val cost = System.currentTimeMillis() - start
                    if (cost > 500) {
                        val timeoutCount = taskTimeoutCountMap.putIfAbsent(taskId, AtomicInteger(1))
                        timeoutCount?.incrementAndGet()
                    }
                }
            }

            override fun toString(): String = "Task Runnable, taskId:$taskId"
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskTriggerPoolHelper::class.java)
    }
}
