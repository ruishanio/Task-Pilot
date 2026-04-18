package com.ruishanio.taskpilot.tool.test.id

import com.ruishanio.taskpilot.tool.id.DateIdTool
import org.junit.jupiter.api.Test

/**
 * DateIdTool 基础调用验证。
 */
class DateIdToolTest {
    @Test
    fun test() {
        println(DateIdTool.getDateId())
    }
}
