package com.ruishanio.taskpilot.tool.test.concurrent

import com.ruishanio.taskpilot.tool.concurrent.TimeWheel
import com.ruishanio.taskpilot.tool.core.DateTool
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit

/**
 * TimeWheel 定时调度行为验证。
 */
class TimeWheelTest {
    @Test
    fun test() {
        val timeWheel = TimeWheel(60, 1000)
        timeWheel.start()
        println("start at:${DateTool.format(Date(), "yyyy-MM-dd HH:mm:ss SSS")}")

        val waitTimes = listOf(1000, 2000, 3000, 5000, 7000)
        for (waitTime in waitTimes) {
            val result =
                timeWheel.submitTask(System.currentTimeMillis() + waitTime) {
                    println("run delay $waitTime at ${DateTool.format(Date(), "yyyy-MM-dd HH:mm:ss SSS")})")
                }
            Assertions.assertTrue(result)
        }

        val result =
            timeWheel.submitTask(System.currentTimeMillis() + 100 * 1000) {
                println("This should not execute")
            }
        Assertions.assertFalse(result)

        Thread.sleep(10 * 1000L)
    }

    @Test
    fun test2() {
        val delayQueue = DelayQueue<DelayedTask>()
        println("start at:${DateTool.format(Date(), "yyyy-MM-dd HH:mm:ss SSS")}")

        val waitTimes = listOf(1000, 2000, 3000, 5000, 7000)
        for (waitTime in waitTimes) {
            delayQueue.add(
                DelayedTask(waitTime.toLong()) {
                    println("run delay $waitTime at ${DateTool.format(Date(), "yyyy-MM-dd HH:mm:ss SSS")})")
                },
            )
        }

        while (delayQueue.isNotEmpty()) {
            val task = delayQueue.take()
            task.execute()
        }
    }

    /**
     * 使用 DelayQueue 对照验证时间轮的调度时机。
     */
    private class DelayedTask(
        delay: Long,
        private val task: Runnable,
    ) : Delayed {
        private val expirationTime: Long = System.currentTimeMillis() + delay

        fun execute() {
            task.run()
        }

        override fun getDelay(unit: TimeUnit): Long =
            unit.convert(expirationTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS)

        override fun compareTo(other: Delayed): Int = expirationTime.compareTo((other as DelayedTask).expirationTime)
    }
}
