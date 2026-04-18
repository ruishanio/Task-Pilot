package com.ruishanio.taskpilot.tool.test.io

import com.ruishanio.taskpilot.tool.io.CsvTool
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.LoggerFactory

/**
 * 覆盖 CSV 读写的基础往返场景，确认 Kotlin 迁移后转义策略不变。
 */
class CsvToolTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun test() {
        val data = mutableListOf<Array<String>>()
        data += arrayOf("用户ID", "用户名", "年龄", "城市", "是否VIP", "新增时间", "别名", "访问量")
        data += arrayOf("1", "张三", "25", "北京", "true", "2023-11-11 11:11:11", "Jack", "100")
        data += arrayOf("2", "李四", "30", "上海", "false", "2023-11-11 11:11:11", "Rose", "200")
        data += arrayOf("3", "王五,测试", "28", "广州", "true", "2023-11-11 11:11:11", "Rose", "200")

        for (i in 0 until 10_000) {
            data += arrayOf(i.toString(), "用户$i", "20", "北京", "true", "2023-11-11 11:11:11", "别名$i", "100")
        }

        val filePath = tempDir.resolve("demo-csv.csv").absolutePath
        CsvTool.writeCsv(filePath, data)
        logger.info("CSV文件已写入: {}", filePath)

        val readData = CsvTool.readCsv(filePath)
        logger.info("读取的CSV数据:")
        readData.forEach { row ->
            logger.info(row.joinToString(" | "))
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CsvToolTest::class.java)
    }
}
