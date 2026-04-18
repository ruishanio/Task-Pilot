package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.ArrayTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * ArrayTool 基础行为验证。
 */
class ArrayToolTest {
    @Test
    fun test() {
        logger.info("test: {}", ArrayTool.isEmpty(null))
        logger.info("test: {}", ArrayTool.isNotEmpty(null))
        logger.info("test: {}", ArrayTool.contains(null, null))

        val values = arrayOf(1, 2, 3)
        logger.info("test2: {}", ArrayTool.isEmpty(values))
        logger.info("test2: {}", ArrayTool.isNotEmpty(values))
        logger.info("test2: {}", ArrayTool.contains(values, 2))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArrayToolTest::class.java)
    }
}
