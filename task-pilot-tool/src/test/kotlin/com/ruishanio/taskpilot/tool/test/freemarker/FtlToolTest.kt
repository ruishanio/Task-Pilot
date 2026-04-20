package com.ruishanio.taskpilot.tool.test.freemarker

import com.ruishanio.taskpilot.tool.freemarker.FtlTool
import freemarker.template.TemplateException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.HashMap

/**
 * FtlTool 模板渲染验证。
 */
class FtlToolTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Throws(TemplateException::class, IOException::class)
    fun test() {
        // 模板文件按测试临时目录动态生成，避免要求开发机提前准备外部模板资源。
        Files.writeString(tempDir.resolve("test.ftl"), "hello ${'$'}{name}!")
        FtlTool.init(tempDir.toString())

        val params = HashMap<String, Any>()
        params["name"] = "task-pilot"
        val text = FtlTool.processString("test.ftl", params)
        logger.info(text)
        assertEquals("hello task-pilot!", text)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FtlToolTest::class.java)
    }
}
