package com.ruishanio.taskpilot.tool.test.crypto

import com.ruishanio.taskpilot.tool.crypto.Sha256Tool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * Sha256Tool 编码结果验证。
 */
class SHA256ToolTest {
    @Test
    fun test() {
        val input = "test"

        val output = Sha256Tool.sha256(input)
        val output2 = Sha256Tool.sha256(input, "123456")
        logger.info("input:{}, md5:{}", input, output)
        logger.info("input2:{}, md5:{}", input, output2)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SHA256ToolTest::class.java)
    }
}
