package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.ObjectTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * ObjectTool 对象通用能力验证。
 */
class ObjectToolTest {
    @Test
    fun test() {
        logger.info("result = {}", ObjectTool.toString(this))
        logger.info("result = {}", ObjectTool.getIdentityHexString(this))
        logger.info("result = {}", !ObjectTool.equal(1, 2))
        logger.info("result = {}", ObjectTool.equal(this, this))
        logger.info("result = {}", !ObjectTool.equal(2L, 2))
        logger.info("result = {}", ObjectTool.isArray(arrayOf("1", "2")))
        logger.info("result = {}", ObjectTool.isEmpty(emptyArray<String>()))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ObjectToolTest::class.java)
    }
}
