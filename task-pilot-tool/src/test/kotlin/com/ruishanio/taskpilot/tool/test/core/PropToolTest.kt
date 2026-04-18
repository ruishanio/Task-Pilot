package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.PropTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * PropTool 资源与文件属性读取验证。
 */
class PropToolTest {
    @Test
    fun test() {
        val prop = PropTool.loadProp("log4j.properties")
        logger.info(PropTool.getString(prop, "log4j.rootLogger"))
    }

    @Test
    fun test2() {
        val prop = PropTool.loadFileProp("/Users/admin/Downloads/test.properties")
        logger.info(PropTool.getString(prop, "k1"))
        logger.info(PropTool.getString(prop, "k2"))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PropToolTest::class.java)
    }
}
