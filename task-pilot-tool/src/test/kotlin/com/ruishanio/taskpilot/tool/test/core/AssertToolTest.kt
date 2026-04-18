package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.AssertTool
import org.junit.jupiter.api.Test

/**
 * AssertTool 基础断言验证。
 */
class AssertToolTest {
    @Test
    fun isTrueTest() {
        AssertTool.isTrue(true, "not true")
    }

    @Test
    fun isNullTest() {
        AssertTool.notNull(Any(), "not null")
    }
}
