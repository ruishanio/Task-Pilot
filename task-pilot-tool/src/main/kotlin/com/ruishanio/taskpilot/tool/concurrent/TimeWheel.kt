package com.ruishanio.taskpilot.tool.concurrent

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

/**
 * 时间轮。
 * 继续采用固定槽位 + 单独执行线程池的轻量实现，不引入层级时间轮以免改变短周期任务的命中行为。
 */
class TimeWheel(
    private val ticksCount: Int,
    private val tickDuration: Long,
    private val taskExecutor: ExecutorService,
) {
    private val wheelDuration: Long = ticksCount * tickDuration
    private val taskSlots: MutableMap<Int, MutableSet<TimeTask>> = ConcurrentHashMap()

    @Volatile
    private var currentTick: Int = 0

    @Volatile
    private var lastTickTime: Long = 0

    private val running = AtomicBoolean(false)

    constructor(ticksCount: Int, tickDuration: Long) : this(ticksCount, tickDuration, 10, 200, 2000)

    constructor(
        ticksCount: Int,
        tickDuration: Long,
        taskExecutorCoreSize: Int,
        taskExecutorMaxSize: Int,
        taskExecutorQueueSize: Int,
    ) : this(
        ticksCount,
        tickDuration,
        ThreadPoolExecutor(
            taskExecutorCoreSize,
            taskExecutorMaxSize,
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(taskExecutorQueueSize),
        ),
    )

    init {
        if (ticksCount < 1 || tickDuration < 1) {
            throw IllegalArgumentException("ticksCount and tickDuration must be greater than 0")
        }
        for (i in 0 until ticksCount) {
            taskSlots[i] = ConcurrentHashMap.newKeySet()
        }
    }

    /**
     * 任务对象保持引用语义，不重写 equals/hashCode，避免同时间同 runnable 被集合去重。
     */
    private class TimeTask(
        val executeTime: Long,
        val task: Runnable,
    )

    fun start() {
        if (running.compareAndSet(false, true)) {
            val now = System.currentTimeMillis()
            lastTickTime = now - (now % 1000)

            Thread {
                while (running.get()) {
                    val current = System.currentTimeMillis()
                    val diff = current - lastTickTime
                    if (diff >= tickDuration) {
                        val ticksToMove = (diff / tickDuration).toInt()
                        for (i in 0 until ticksToMove) {
                            val tickToProcess = (currentTick + i) % ticksCount
                            executeTasks(tickToProcess)
                        }
                        currentTick = (currentTick + ticksToMove) % ticksCount
                        lastTickTime += ticksToMove * tickDuration
                    }

                    try {
                        Thread.sleep(kotlin.math.max(1, tickDuration - (System.currentTimeMillis() - lastTickTime)))
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }.apply {
                isDaemon = true
                start()
            }

            Runtime.getRuntime().addShutdownHook(Thread { stop() })
            logger.info(">>>>>>>>>>> TimeWheel[hashCode = {}] 已启动。", this.hashCode())
        }
    }

    fun stop() {
        running.set(false)
        taskExecutor.shutdown()
        try {
            if (!taskExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                taskExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            taskExecutor.shutdownNow()
            logger.error(">>>>>>>>>>> TimeWheel[hashCode = {}] 停止时发生异常。", this.hashCode(), e)
        }
        logger.info(">>>>>>>>>>> TimeWheel[hashCode = {}] 已停止。", this.hashCode())
    }

    fun submitTask(
        executeTime: Long,
        task: Runnable,
    ): Boolean {
        val now = System.currentTimeMillis()
        if (executeTime <= now || executeTime > now + wheelDuration) {
            return false
        }

        val timeTask = TimeTask(executeTime, task)
        val targetTick = calculateTick(executeTime)
        taskSlots[targetTick]!!.add(timeTask)
        return true
    }

    private fun calculateTick(executeTime: Long): Int {
        val delay = executeTime - System.currentTimeMillis()
        val ticks = (delay / tickDuration).toInt()
        return (currentTick + ticks) % ticksCount
    }

    /**
     * 槽位执行时继续只提交到执行线程池，避免时间轮线程被业务任务阻塞。
     */
    private fun executeTasks(tick: Int) {
        val tasks = taskSlots[tick]!!
        val now = System.currentTimeMillis()
        tasks.removeIf { task ->
            if (task.executeTime <= now) {
                try {
                    taskExecutor.submit(task.task)
                } catch (e: Exception) {
                    logger.error(">>>>>>>>>>> TimeWheel[hashCode = {}] 提交定时任务时发生异常。", this.hashCode(), e)
                }
                true
            } else {
                false
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TimeWheel::class.java)
    }
}
