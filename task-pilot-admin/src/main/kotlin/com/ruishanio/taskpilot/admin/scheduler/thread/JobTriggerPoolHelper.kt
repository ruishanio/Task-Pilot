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
class JobTriggerPoolHelper {
    private lateinit var fastTriggerPool: ThreadPoolExecutor
    private lateinit var slowTriggerPool: ThreadPoolExecutor

    @Volatile
    private var minTim: Long = System.currentTimeMillis() / 60000

    @Volatile
    private var jobTimeoutCountMap: ConcurrentMap<Int, AtomicInteger> = ConcurrentHashMap()

    fun start() {
        fastTriggerPool = ThreadPoolExecutor(
            10,
            TaskPilotAdminBootstrap.instance.triggerPoolFastMax,
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(2000),
            ThreadFactory { runnable ->
                Thread(runnable, "task-pilot, admin JobTriggerPoolHelper-fastTriggerPool-${runnable.hashCode()}")
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
                Thread(runnable, "task-pilot, admin JobTriggerPoolHelper-slowTriggerPool-${runnable.hashCode()}")
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
        jobId: Int,
        triggerType: TriggerTypeEnum,
        failRetryCount: Int,
        executorShardingParam: String?,
        executorParam: String?,
        addressList: String?
    ) {
        var triggerPool = fastTriggerPool
        val jobTimeoutCount = jobTimeoutCountMap[jobId]
        if (jobTimeoutCount != null && jobTimeoutCount.get() > 10) {
            triggerPool = slowTriggerPool
        }

        triggerPool.execute(object : Runnable {
            override fun run() {
                val start = System.currentTimeMillis()
                try {
                    TaskPilotAdminBootstrap.instance.jobTrigger.trigger(
                        jobId,
                        triggerType,
                        failRetryCount,
                        executorShardingParam,
                        executorParam,
                        addressList
                    )
                } catch (e: Throwable) {
                    logger.error("触发任务时发生异常。jobId={}, triggerType={}", jobId, triggerType, e)
                } finally {
                    val minTimNow = System.currentTimeMillis() / 60000
                    if (minTim != minTimNow) {
                        minTim = minTimNow
                        jobTimeoutCountMap.clear()
                    }

                    val cost = System.currentTimeMillis() - start
                    if (cost > 500) {
                        val timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId, AtomicInteger(1))
                        timeoutCount?.incrementAndGet()
                    }
                }
            }

            override fun toString(): String = "Job Runnable, jobId:$jobId"
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobTriggerPoolHelper::class.java)
    }
}
