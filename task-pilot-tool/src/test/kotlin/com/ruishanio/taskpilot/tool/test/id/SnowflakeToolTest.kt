package com.ruishanio.taskpilot.tool.test.id

import com.ruishanio.taskpilot.tool.id.SnowflakeIdTool
import org.junit.jupiter.api.Test

/**
 * SnowflakeIdTool 生成结果验证。
 */
class SnowflakeToolTest {
    @Test
    fun test() {
        val idGen = SnowflakeIdTool(1)
        repeat(10) {
            println(idGen.nextId())
        }
    }
}
