package com.ruishanio.taskpilot.tool.test.concurrent

import com.ruishanio.taskpilot.tool.concurrent.MessageQueue
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicLong

/**
 * MessageQueue 批量消费行为验证。
 */
class MessageQueueTest {
    @Test
    fun test1() {
        val messageQueue =
            MessageQueue<String>(
                "demoQueue",
                { messages: List<String> -> println("Consume: $messages") },
                3,
                3,
            )

        for (index in 0 until 1000) {
            messageQueue.produce("Message-$index")
        }

        messageQueue.stop()
    }

    @Test
    fun test2() {
        val consumeCount = AtomicLong(0)
        val messageQueue =
            MessageQueue<String>(
                "demoQueue",
                { messages: List<String> ->
                    for (message in messages) {
                        consumeCount.incrementAndGet()
                    }
                },
                100,
                10,
            )

        val startTime = System.currentTimeMillis()
        val count = 1000000L
        for (index in 0 until count) {
            messageQueue.produce("test-$index")
        }

        messageQueue.stop()
        val cost = System.currentTimeMillis() - startTime

        Assertions.assertEquals(count, consumeCount.get())
        println("Final count = ${consumeCount.get()}, cost = $cost, tps = ${consumeCount.get() * 1000 / cost}")
    }
}
