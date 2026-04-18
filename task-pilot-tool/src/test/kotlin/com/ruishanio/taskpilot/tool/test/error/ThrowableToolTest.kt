package com.ruishanio.taskpilot.tool.test.error

import com.ruishanio.taskpilot.tool.error.ThrowableTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * ThrowableTool 异常字符串化验证。
 */
class ThrowableToolTest {
    @Test
    fun test() {
        logger.info("error : {}", ThrowableTool.toString(Exception("test")))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThrowableToolTest::class.java)
    }
}
