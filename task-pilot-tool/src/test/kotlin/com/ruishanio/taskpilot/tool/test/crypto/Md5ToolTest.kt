package com.ruishanio.taskpilot.tool.test.crypto

import com.ruishanio.taskpilot.tool.crypto.Md5Tool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * Md5Tool 编码结果验证。
 */
class Md5ToolTest {
    @Test
    fun test() {
        val input = "test"

        val output = Md5Tool.md5(input)
        val output2 = Md5Tool.md5(input, "123456")
        logger.info("input:{}, md5:{}", input, output)
        logger.info("input2:{}, md5:{}", input, output2)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Md5ToolTest::class.java)
    }
}
