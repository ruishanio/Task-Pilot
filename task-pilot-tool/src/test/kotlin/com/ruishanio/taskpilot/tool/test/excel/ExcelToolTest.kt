package com.ruishanio.taskpilot.tool.test.excel

import com.ruishanio.taskpilot.tool.excel.ExcelTool
import com.ruishanio.taskpilot.tool.io.FileTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.test.excel.model.ShopDTO
import com.ruishanio.taskpilot.tool.test.excel.model.UserDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Supplier
import java.nio.file.Path
import kotlin.jvm.JvmSuppressWildcards

/**
 * ExcelTool 读写与流式接口验证。
 */
class ExcelToolTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun write_read_default() {
        val userDTOList = (1..10).map { UserDTO(it.toLong(), "用户$it") }

        val filePath = tempFile("demo-sheet-02.xlsx")
        FileTool.delete(filePath)
        ExcelTool.writeExcel(filePath, userDTOList)

        val list3 = ExcelTool.readExcel(filePath, UserDTO::class.java)
        logger.info("{}:{}", list3?.size, list3)
        assertEquals(userDTOList.size, list3?.size)
    }

    @Test
    fun write_read_multiSheet() {
        val shopDTOList =
            (0 until 10).map { index ->
                ShopDTO(
                    true,
                    "商户$index",
                    index.toShort(),
                    1000 + index,
                    (10000 + index).toLong(),
                    (1000 + index).toFloat(),
                    (10000 + index).toDouble(),
                    Date(),
                    "备注$index",
                )
            }

        val userDTOList = (1..10).map { UserDTO(it.toLong(), "用户$it") }

        val filePath = tempFile("demo-sheet.xlsx")
        FileTool.delete(filePath)
        ExcelTool.writeExcel(filePath, shopDTOList, userDTOList)

        val list2 = ExcelTool.readExcel(filePath, ShopDTO::class.java)
        logger.info("{}:{}", list2?.size, list2)
        assertEquals(shopDTOList.size, list2?.size)

        val list3 = ExcelTool.readExcel(filePath, UserDTO::class.java)
        logger.info("{}:{}", list3?.size, list3)
        assertEquals(userDTOList.size, list3?.size)
    }

    @Test
    fun readStream() {
        val userDTOList = (1..10).map { UserDTO(it.toLong(), "用户$it") }

        val filePath = tempFile("demo-sheet-02.xlsx")
        FileTool.delete(filePath)
        ExcelTool.writeExcel(filePath, userDTOList)

        // 这里用匿名对象而不是 lambda，确保运行时还能拿到 `Consumer<UserDTO>` 的泛型签名。
        ExcelTool.readExcel(
            filePath,
            object : Consumer<UserDTO> {
                override fun accept(userDTO: UserDTO) {
                    logger.info("item: {}", GsonTool.toJson(userDTO))
                }
            },
        )
    }

    @Test
    fun writeStream() {
        val filePath = tempFile("demo-sheet-02.xlsx")
        FileTool.delete(filePath)

        val total = AtomicInteger(0)
        val userSupplier =
            object : Supplier<UserDTO?> {
                override fun get(): UserDTO? {
                    return if (total.incrementAndGet() > 15) {
                        null
                    } else {
                        UserDTO(total.get().toLong(), "用户${total.get()}")
                    }
                }
            }

        ExcelTool.writeExcel(filePath, userSupplier)
        val list = ExcelTool.readExcel(filePath, UserDTO::class.java)
        logger.info("readExcel:{}", list)
        assertEquals(15, list?.size)
    }

    @Test
    fun writeStream2() {
        val filePath = tempFile("demo-sheet-02.xlsx")
        FileTool.delete(filePath)

        val total = AtomicInteger(0)
        val userSupplier =
            object : Supplier<List<@JvmSuppressWildcards UserDTO>?> {
                override fun get(): List<@JvmSuppressWildcards UserDTO>? {
                    return if (total.incrementAndGet() > 2) {
                        null
                    } else {
                        listOf(
                            UserDTO((total.get() * 10 + 1).toLong(), "用户1"),
                            UserDTO((total.get() * 10 + 2).toLong(), "用户2"),
                            UserDTO((total.get() * 10 + 3).toLong(), "用户3"),
                        )
                    }
                }
            }

        ExcelTool.writeExcel(filePath, userSupplier)
        val list = ExcelTool.readExcel(filePath, UserDTO::class.java)
        logger.info("readExcel:{}", list)
        assertEquals(6, list?.size)
    }

    /**
     * 测试文件统一落到 JUnit 临时目录，避免依赖开发机上的固定下载目录。
     */
    private fun tempFile(fileName: String): String = tempDir.resolve(fileName).toString()

    companion object {
        private val logger = LoggerFactory.getLogger(ExcelToolTest::class.java)
    }
}
