package com.ruishanio.taskpilot.tool.test.id

import com.ruishanio.taskpilot.tool.id.UUIDTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * UUIDTool 生成结果验证。
 */
class UUIDToolTest {
    @Test
    fun test() {
        repeat(10) {
            logger.info("uuid={}", UUIDTool.getUUID())
        }

        repeat(10) {
            logger.info("uuid2={}", UUIDTool.getSimpleUUID())
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UUIDToolTest::class.java)
    }
}
