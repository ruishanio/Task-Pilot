package com.ruishanio.taskpilot.tool.test.concurrent

import com.ruishanio.taskpilot.tool.concurrent.CyclicThread
import com.ruishanio.taskpilot.tool.core.DateTool
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * CyclicThread 周期线程行为验证。
 */
class CyclicThreadTest {
    @Test
    fun test1() {
        val threadHelper =
            CyclicThread(
                "demoCyclicThread",
                Runnable {
                    println("thread running at ${DateTool.formatDateTime(Date())}")
                },
                200,
            )

        threadHelper.start()
        TimeUnit.SECONDS.sleep(3)

        threadHelper.stop()
    }

    @Test
    fun test2() {
        CyclicThread(
            "demoCyclicThread-11111",
            Runnable {
                println("${Thread.currentThread().name}: running at ${DateTool.formatDateTime(Date())}")
            },
            5 * 1000,
            true,
        ).start()
        TimeUnit.SECONDS.sleep(2)

        CyclicThread(
            "demoCyclicThread-22222",
            Runnable {
                println("${Thread.currentThread().name}: running at ${DateTool.formatDateTime(Date())}")
            },
            5 * 1000,
            true,
        ).start()

        TimeUnit.SECONDS.sleep(20)
    }
}
