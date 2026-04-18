package com.ruishanio.taskpilot.tool.test.crypto

import com.ruishanio.taskpilot.tool.crypto.Base64Tool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * Base64Tool 编解码验证。
 */
class Base64ToolTest {
    @Test
    fun test() {
        val str = "1234567890"
        logger.info(str)

        logger.info(Base64Tool.encodeStandard(str))
        logger.info(Base64Tool.decodeStandard(Base64Tool.encodeStandard(str)))

        logger.info(Base64Tool.encodeUrlSafe(str))
        logger.info(Base64Tool.decodeUrlSafe(Base64Tool.encodeUrlSafe(str)))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Base64ToolTest::class.java)
    }
}
