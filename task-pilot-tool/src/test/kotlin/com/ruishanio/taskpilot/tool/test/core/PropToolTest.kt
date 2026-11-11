package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.PropTool
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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
    fun test2(@TempDir directory: Path) {
        // 测试自行创建配置文件，不依赖开发者电脑上的目录或文件。
        val file = directory.resolve("test.properties")
        Files.writeString(file, "k1=value1\nk2=value2\n")
        val prop = PropTool.loadFileProp(file.toString())
        assertEquals("value1", PropTool.getString(prop, "k1"))
        assertEquals("value2", PropTool.getString(prop, "k2"))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PropToolTest::class.java)
    }
}
