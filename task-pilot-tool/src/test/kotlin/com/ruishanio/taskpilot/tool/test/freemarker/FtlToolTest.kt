package com.ruishanio.taskpilot.tool.test.freemarker

import com.ruishanio.taskpilot.tool.freemarker.FtlTool
import freemarker.template.TemplateException
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.HashMap

/**
 * FtlTool 模板渲染验证。
 */
class FtlToolTest {
    @Test
    @Throws(TemplateException::class, IOException::class)
    fun test() {
        FtlTool.init("/Users/admin/Downloads/")

        val text = FtlTool.processString("test.ftl", HashMap())
        logger.info(text)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FtlToolTest::class.java)
    }
}
