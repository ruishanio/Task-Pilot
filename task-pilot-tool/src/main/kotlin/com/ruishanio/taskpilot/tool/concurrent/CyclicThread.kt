package com.ruishanio.taskpilot.tool.concurrent

import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

/**
 * 周期线程。
 * 继续保留基于原生线程的一次性启动模型，避免迁移后把重复 `start()` 的历史行为改掉。
 */
class CyclicThread(
    private val name: String,
    private val daemon: Boolean,
    runnable: Runnable,
    private val cyclicInterval: Long,
    private val alignTime: Boolean,
) {
    @Volatile
    private var isRunning: Boolean = false
    private val lock = Any()
    private val workerThread: Thread

    constructor(name: String, runnable: Runnable, cyclicInterval: Long) : this(name, true, runnable, cyclicInterval, false)

    constructor(name: String, runnable: Runnable, cyclicInterval: Long, alignTime: Boolean) :
        this(name, true, runnable, cyclicInterval, alignTime)

    constructor(name: String, daemon: Boolean, runnable: Runnable, cyclicInterval: Long) :
        this(name, daemon, runnable, cyclicInterval, false)

    init {
        workerThread =
            Thread(CycleRunnable(runnable, this)).apply {
                isDaemon = daemon
                setName(name)
            }
    }

    /**
     * 循环执行逻辑和等待逻辑拆开，保证业务异常不会打断整个周期线程。
     */
    private class CycleRunnable(
        private val runnable: Runnable,
        private val cyclicThread: CyclicThread,
    ) : Runnable {
        override fun run() {
            logger.info(">>>>>>>>>>> CyclicThread[name = {}] 已启动。", cyclicThread.name)

            while (cyclicThread.isRunning) {
                try {
                    runnable.run()
                } catch (e: Throwable) {
                    if (cyclicThread.isRunning) {
                        logger.error(">>>>>>>>>>> CyclicThread[name = {}] 执行循环任务时发生异常。", cyclicThread.name, e)
                    }
                }

                if (cyclicThread.isRunning) {
                    try {
                        if (cyclicThread.alignTime) {
                            val startTime = System.currentTimeMillis()
                            TimeUnit.MILLISECONDS.sleep(cyclicThread.cyclicInterval - (startTime % cyclicThread.cyclicInterval))
                        } else {
                            TimeUnit.MILLISECONDS.sleep(cyclicThread.cyclicInterval)
                        }
                    } catch (e: Throwable) {
                        if (cyclicThread.isRunning) {
                            logger.error(">>>>>>>>>>> CyclicThread[name = {}] 等待下一次循环时发生异常。", cyclicThread.name, e)
                        }
                    }
                }
            }
            logger.info(">>>>>>>>>>> CyclicThread[name = {}] 已停止。", cyclicThread.name)
        }
    }

    fun start() {
        synchronized(lock) {
            if (!isRunning) {
                isRunning = true
                workerThread.start()
                Runtime.getRuntime().addShutdownHook(Thread { stop() })
            }
        }
    }

    /**
     * 停止时继续先打断再等待，尽量保持和旧实现相同的退出顺序。
     */
    fun stop() {
        synchronized(lock) {
            if (isRunning) {
                isRunning = false
                if (workerThread.state != Thread.State.TERMINATED) {
                    workerThread.interrupt()
                    try {
                        workerThread.join()
                    } catch (e: Throwable) {
                        logger.error(">>>>>>>>>>> CyclicThread[name = {}] 停止线程并等待结束时发生异常。", name, e)
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CyclicThread::class.java)
    }
}
