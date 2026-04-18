package com.ruishanio.taskpilot.tool.test.excel

import com.ruishanio.taskpilot.tool.excel.ExcelTool
import com.ruishanio.taskpilot.tool.io.FileTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import com.ruishanio.taskpilot.tool.test.excel.model.ShopDTO
import com.ruishanio.taskpilot.tool.test.excel.model.UserDTO
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * ExcelTool 读写与流式接口验证。
 */
class ExcelToolTest {
    @Test
    fun write_read_default() {
        val userDTOList = (1..10).map { UserDTO(it.toLong(), "用户$it") }

        val filePath = "/Users/admin/Downloads/excel/demo-sheet-02.xlsx"
        FileTool.delete(filePath)
        ExcelTool.writeExcel(filePath, userDTOList)

        val list3 = ExcelTool.readExcel(filePath, UserDTO::class.java)
        logger.info("{}:{}", list3?.size, list3)
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

        val filePath = "/Users/admin/Downloads/excel/demo-sheet.xlsx"
        FileTool.delete(filePath)
        ExcelTool.writeExcel(filePath, shopDTOList, userDTOList)

        val list2 = ExcelTool.readExcel(filePath, ShopDTO::class.java)
        logger.info("{}:{}", list2?.size, list2)

        val list3 = ExcelTool.readExcel(filePath, UserDTO::class.java)
        logger.info("{}:{}", list3?.size, list3)
    }

    @Test
    fun readStream() {
        val userDTOList = (1..10).map { UserDTO(it.toLong(), "用户$it") }

        val filePath = "/Users/admin/Downloads/excel/demo-sheet-02.xlsx"
        FileTool.delete(filePath)
        ExcelTool.writeExcel(filePath, userDTOList)

        ExcelTool.readExcel(filePath, Consumer<UserDTO> { userDTO -> logger.info("item: {}", GsonTool.toJson(userDTO)) })
    }

    @Test
    fun writeStream() {
        val filePath = "/Users/admin/Downloads/excel/demo-sheet-02.xlsx"
        FileTool.delete(filePath)

        val total = AtomicInteger(0)
        val userSupplier =
            Supplier<UserDTO?> {
                if (total.incrementAndGet() > 15) {
                    null
                } else {
                    UserDTO(total.get().toLong(), "用户${total.get()}")
                }
            }

        ExcelTool.writeExcel(filePath, userSupplier)
        logger.info("readExcel:{}", ExcelTool.readExcel(filePath, UserDTO::class.java))
    }

    @Test
    fun writeStream2() {
        val filePath = "/Users/admin/Downloads/excel/demo-sheet-02.xlsx"
        FileTool.delete(filePath)

        val total = AtomicInteger(0)
        val userSupplier =
            Supplier<List<UserDTO>?> {
                if (total.incrementAndGet() > 2) {
                    null
                } else {
                    listOf(
                        UserDTO((total.get() * 10 + 1).toLong(), "用户1"),
                        UserDTO((total.get() * 10 + 2).toLong(), "用户2"),
                        UserDTO((total.get() * 10 + 3).toLong(), "用户3"),
                    )
                }
            }

        ExcelTool.writeExcel(filePath, userSupplier)
        logger.info("readExcel:{}", ExcelTool.readExcel(filePath, UserDTO::class.java))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExcelToolTest::class.java)
    }
}
