package com.ruishanio.taskpilot.tool.test.concurrent

import com.ruishanio.taskpilot.tool.concurrent.TokenBucket
import com.ruishanio.taskpilot.tool.core.DateTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * TokenBucket 基础限流行为验证。
 */
class TokenBucketTest {
    @Test
    fun test() {
        val smoothBursty = TokenBucket.create(5.0)
        repeat(10) {
            val cost = smoothBursty.acquire()
            logger.info("{}: cost {}", DateTool.formatDateTime(Date()), cost)
        }
    }

    @Test
    fun test2() {
        val smoothBursty = TokenBucket.create(5.0)
        repeat(10) {
            val result = smoothBursty.tryAcquire(100, TimeUnit.MILLISECONDS)
            logger.info("{}: result {}", DateTool.formatDateTime(Date()), result)
        }
    }

    @Test
    fun test3() {
        val smoothWarmingUp = TokenBucket.create(5.0, 2, TimeUnit.SECONDS)
        repeat(10) {
            val cost = smoothWarmingUp.acquire()
            logger.info("{}: cost {}", DateTool.formatDateTime(Date()), cost)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TokenBucketTest::class.java)
    }
}
