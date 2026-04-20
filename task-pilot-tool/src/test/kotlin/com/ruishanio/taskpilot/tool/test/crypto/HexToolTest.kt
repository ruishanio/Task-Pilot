package com.ruishanio.taskpilot.tool.test.crypto

import com.ruishanio.taskpilot.tool.crypto.HexTool
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * HexTool 编解码验证。
 */
class HexToolTest {
    @Test
    fun test() {
        val input = "task-pilot-tool"
        val output = HexTool.toHex(input)
        logger.info("input: {}, output: {}", input, output)

        val input2 = HexTool.fromHex(output)
        logger.info("calculate input2: {}", input2)

        Assertions.assertEquals(input, input2)
    }

    @Test
    fun shouldSupportUppercaseAndLowercaseHexInput() {
        // 回归测试：十六进制字符解码需要同时兼容大小写输入。
        val lowercaseHex = "7461736b2d70696c6f742d746f6f6c"
        val uppercaseHex = lowercaseHex.uppercase()

        Assertions.assertEquals("task-pilot-tool", HexTool.fromHex(lowercaseHex))
        Assertions.assertEquals("task-pilot-tool", HexTool.fromHex(uppercaseHex))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HexToolTest::class.java)
    }
}
