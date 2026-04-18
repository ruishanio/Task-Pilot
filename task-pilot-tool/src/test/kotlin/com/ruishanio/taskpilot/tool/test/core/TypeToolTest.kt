package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.TypeTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * TypeTool 类型可赋值关系验证。
 */
class TypeToolTest {
    @Test
    fun test() {
        logger.info("{}", TypeTool.isAssignable(Number::class.java, Long::class.java))
        logger.info("{}", TypeTool.isAssignable(Long::class.java, Long::class.java))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TypeToolTest::class.java)
    }
}
