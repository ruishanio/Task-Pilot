package com.ruishanio.taskpilot.tool.excel

import com.ruishanio.taskpilot.tool.core.ArrayTool
import com.ruishanio.taskpilot.tool.core.AssertTool
import com.ruishanio.taskpilot.tool.core.CollectionTool
import com.ruishanio.taskpilot.tool.core.StringTool
import com.ruishanio.taskpilot.tool.excel.annotation.ExcelField
import com.ruishanio.taskpilot.tool.excel.annotation.ExcelSheet
import com.ruishanio.taskpilot.tool.excel.util.ExcelFieldUtil
import com.ruishanio.taskpilot.tool.io.FileTool
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.nio.file.Files
import java.util.ArrayList
import java.util.HashMap
import java.util.function.Consumer
import java.util.function.Supplier
import org.apache.poi.EncryptedDocumentException
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook

/**
 * Excel 导入导出工具。
 * 继续走“注解 + 反射”模型，避免上层 DTO 为了迁移 Kotlin 被迫改动字段定义或映射规则。
 */
object ExcelTool {
    private val formatter = DataFormatter()

    private fun createWorkbook(sheetDataList: Array<out kotlin.collections.List<*>>): Workbook {
        if (sheetDataList.isEmpty()) {
            throw RuntimeException("ExcelTool createWorkbook error, sheetData can not be empty.")
        }

        val workbook: Workbook = XSSFWorkbook()
        for (sheetData in sheetDataList) {
            val sheetClass = sheetData[0]!!.javaClass
            createSheet(workbook, sheetClass, sheetData, null)
        }
        return workbook
    }

    private fun createWorkbook(suppliers: Array<out Supplier<*>>): Workbook {
        if (suppliers.isEmpty()) {
            throw RuntimeException("ExcelTool createWorkbook error, consumers can not be empty.")
        }

        val workbook: Workbook = XSSFWorkbook()
        for (supplier in suppliers) {
            val parameterizedType = supplier.javaClass.genericInterfaces[0] as ParameterizedType
            val actualType = parameterizedType.actualTypeArguments[0]
            val sheetClass =
                if (actualType is ParameterizedType && actualType.rawType == List::class.java) {
                    actualType.actualTypeArguments[0] as Class<*>
                } else {
                    actualType as Class<*>
                }
            createSheet(workbook, sheetClass, null, supplier)
        }
        return workbook
    }

    /**
     * Sheet 创建逻辑继续一次性解析字段定义，避免在逐行输出时重复做反射扫描。
     */
    private fun createSheet(
        workbook: Workbook,
        sheetClass: Class<*>,
        sheetData: List<*>?,
        supplier: Supplier<*>?,
    ) {
        AssertTool.notNull(workbook, "workbook can not be null.")
        AssertTool.notNull(sheetClass, "sheetClass can not be null.")

        val excelSheetAnno = sheetClass.getAnnotation(ExcelSheet::class.java)
        var sheetName = resolveSheetName(sheetClass, excelSheetAnno)
        val headColorIndex = excelSheetAnno?.headColor?.index ?: (-1).toShort()
        val fields = collectSheetFields(sheetClass)
        if (fields.isEmpty()) {
            throw RuntimeException("ExcelTool createSheet error, sheetClass fields can not be empty.")
        }

        if (workbook.getSheet(sheetName) != null) {
            for (i in 2..1000) {
                val newSheetName = sheetName + i
                if (workbook.getSheet(newSheetName) == null) {
                    sheetName = newSheetName
                    break
                }
            }
        }
        val sheet = workbook.createSheet(sheetName)
        val fieldWidthArr = IntArray(fields.size)
        val fieldDataStyleArr = arrayOfNulls<CellStyle>(fields.size)
        val headRow = sheet.createRow(0)

        for (i in fields.indices) {
            val field = fields[i]
            val excelFieldAnno = field.getAnnotation(ExcelField::class.java)
            var fieldName = field.name
            var fieldWidth = 0
            var align: HorizontalAlignment? = null
            if (excelFieldAnno != null) {
                if (excelFieldAnno.name.trim().isNotEmpty()) {
                    fieldName = excelFieldAnno.name.trim()
                }
                fieldWidth = excelFieldAnno.width
                align = excelFieldAnno.align
            }

            fieldWidthArr[i] = fieldWidth
            val fieldDataStyle = workbook.createCellStyle()
            if (align != null) {
                fieldDataStyle.alignment = align
            }
            fieldDataStyleArr[i] = fieldDataStyle

            val headStyle = workbook.createCellStyle()
            headStyle.cloneStyleFrom(fieldDataStyle)
            if (headColorIndex > (-1).toShort()) {
                headStyle.fillForegroundColor = headColorIndex
                headStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            }

            val cell = headRow.createCell(i, CellType.STRING)
            cell.cellStyle = headStyle
            cell.setCellValue(fieldName)
        }

        var rowIndex = 0
        if (CollectionTool.isNotEmpty(sheetData)) {
            for (rowData in sheetData!!) {
                writeRowData(sheet, fields, fieldDataStyleArr, rowIndex++, rowData)
            }
        }
        if (supplier != null) {
            var rowData = supplier.get()
            while (rowData != null) {
                if (rowData is kotlin.collections.List<*>) {
                    for (rowListItem in rowData) {
                        writeRowData(sheet, fields, fieldDataStyleArr, rowIndex++, rowListItem)
                    }
                } else {
                    writeRowData(sheet, fields, fieldDataStyleArr, rowIndex++, rowData)
                }
                rowData = supplier.get()
            }
        }

        writeColumnWidth(sheet, fields, fieldWidthArr)
    }

    private fun writeRowData(
        sheet: Sheet,
        fields: kotlin.collections.List<Field>,
        fieldDataStyleArr: Array<CellStyle?>,
        index: Int,
        rowData: Any?,
    ) {
        val row = sheet.createRow(index + 1)
        for (i in fields.indices) {
            try {
                val field = fields[i]
                field.isAccessible = true
                val fieldValue = field.get(rowData)
                val fieldValueString = ExcelFieldUtil.formatValue(field, fieldValue)

                val cell = row.createCell(i, CellType.STRING)
                cell.cellStyle = fieldDataStyleArr[i]
                cell.setCellValue(fieldValueString)
            } catch (e: IllegalAccessException) {
                throw RuntimeException("ExcelTool createSheet error, write row-data error.", e)
            }
        }
    }

    private fun writeColumnWidth(
        sheet: Sheet,
        fields: kotlin.collections.List<Field>,
        fieldWidthArr: IntArray,
    ) {
        for (i in fields.indices) {
            val fieldWidth = fieldWidthArr[i]
            if (fieldWidth > 0) {
                sheet.setColumnWidth(i, fieldWidth)
            } else {
                sheet.autoSizeColumn(i)
            }
        }
    }
    fun writeExcel(filePath: String, vararg sheetDataList: kotlin.collections.List<*>) {
        if (StringTool.isBlank(filePath)) {
            throw RuntimeException("ExcelTool writeFile error, filePath is empty.")
        }
        validateExcelPath(filePath)

        if (FileTool.exists(filePath)) {
            throw RuntimeException("ExcelTool writeFile error, filePath: $filePath already exists.")
        } else {
            try {
                FileTool.createParentDirectories(FileTool.file(filePath))
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        try {
            createWorkbook(sheetDataList).use { workbook ->
                FileOutputStream(filePath).use { fileOutputStream ->
                    workbook.write(fileOutputStream)
                    fileOutputStream.flush()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("ExcelTool writeFile error, filePath: $filePath", e)
        }
    }
    fun writeExcel(vararg sheetDataList: kotlin.collections.List<*>): ByteArray {
        if (sheetDataList.isEmpty()) {
            throw RuntimeException("ExcelTool writeByteArray error, sheetDataList is empty.")
        }

        try {
            createWorkbook(sheetDataList).use { workbook ->
                ByteArrayOutputStream().use { byteArrayOutputStream ->
                    workbook.write(byteArrayOutputStream)
                    byteArrayOutputStream.flush()
                    return byteArrayOutputStream.toByteArray()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("ExcelTool writeByteArray error.", e)
        }
    }
    fun writeExcel(filePath: String, suppliers: Supplier<*>) {
        if (StringTool.isBlank(filePath)) {
            throw RuntimeException("ExcelTool writeFile error, filePath is empty.")
        }
        validateExcelPath(filePath)

        if (FileTool.exists(filePath)) {
            throw RuntimeException("ExcelTool writeFile error, filePath: $filePath already exists.")
        } else {
            try {
                FileTool.createParentDirectories(FileTool.file(filePath))
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        try {
            createWorkbook(arrayOf(suppliers)).use { workbook ->
                FileOutputStream(filePath).use { fileOutputStream ->
                    workbook.write(fileOutputStream)
                    fileOutputStream.flush()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("ExcelTool writeFile error, filePath: $filePath", e)
        }
    }
    fun writeExcel(vararg suppliers: Supplier<*>): ByteArray {
        if (ArrayTool.isEmpty(suppliers as Array<out Any?>?)) {
            throw RuntimeException("ExcelTool writeByteArray error, sheetDataList is empty.")
        }

        try {
            createWorkbook(suppliers).use { workbook ->
                ByteArrayOutputStream().use { byteArrayOutputStream ->
                    workbook.write(byteArrayOutputStream)
                    byteArrayOutputStream.flush()
                    return byteArrayOutputStream.toByteArray()
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("ExcelTool writeByteArray error.", e)
        }
    }

    /**
     * 读取时继续按表头名和字段注解名称做匹配，不依赖列顺序完全一致。
     */
    private fun <T> readSheet(
        workbook: Workbook,
        sheetClass: Class<T>,
        sheetData: MutableList<T>?,
        consumer: Consumer<T>?,
    ) {
        AssertTool.notNull(workbook, "workbook can not be null.")
        AssertTool.notNull(sheetClass, "sheetClass can not be null.")

        try {
            val sheetName = resolveSheetName(sheetClass, sheetClass.getAnnotation(ExcelSheet::class.java))
            val fields = collectSheetFields(sheetClass)
            if (fields.isEmpty()) {
                throw RuntimeException("ExcelTool readSheet error, sheetClass[${sheetClass.name}] fields can not be empty.")
            }

            val fieldName2FieldMap = HashMap<String, Field>()
            for (field in fields) {
                val excelFieldAnno = field.getAnnotation(ExcelField::class.java)
                val fieldName =
                    if (excelFieldAnno != null && excelFieldAnno.name.trim().isNotEmpty()) {
                        excelFieldAnno.name.trim()
                    } else {
                        field.name
                    }
                fieldName2FieldMap[fieldName] = field
            }

            val sheet = workbook.getSheet(sheetName) ?: return
            val headRow = sheet.getRow(0) ?: return
            val cellIndex2FieldName = HashMap<Int, String>()
            for (i in 0 until headRow.lastCellNum) {
                val cell = headRow.getCell(i) ?: continue
                val cellValueStr = formatter.formatCellValue(cell)
                cellIndex2FieldName[i] = cellValueStr
            }

            val defaultConstructor = findDefaultConstructor(sheetClass)
            val sheetIterator = sheet.rowIterator()
            var rowIndex = 0
            while (sheetIterator.hasNext()) {
                val row = sheetIterator.next()
                if (rowIndex > 0) {
                    val rowObj = defaultConstructor.newInstance()
                    for (i in 0 until headRow.lastCellNum) {
                        val cell = row.getCell(i) ?: continue
                        val field = cellIndex2FieldName[i]?.let { fieldName2FieldMap[it] } ?: continue
                        val cellValueStr = formatter.formatCellValue(cell)
                        val cellValue = ExcelFieldUtil.parseValue(field, cellValueStr) ?: continue

                        field.isAccessible = true
                        field.set(rowObj, cellValue)
                    }

                    if (sheetData != null) {
                        sheetData.add(rowObj)
                    }
                    if (consumer != null) {
                        consumer.accept(rowObj)
                    }
                }
                rowIndex++
            }
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException("ExcelTool readSheet error, ${e.message}", e)
        }
    }
    fun <T> readExcel(inputStream: InputStream, sheetClass: Class<T>): kotlin.collections.List<T> {
        try {
            val workbook = WorkbookFactory.create(inputStream)
            workbook.use {
                val sheetData = ArrayList<T>()
                readSheet(it, sheetClass, sheetData, null)
                return sheetData
            }
        } catch (e: IOException) {
            throw RuntimeException("ExcelTool readExcel error, ${e.message}", e)
        } catch (e: EncryptedDocumentException) {
            throw RuntimeException("ExcelTool readExcel error, ${e.message}", e)
        }
    }
    fun <T> readExcel(excelFile: File, sheetClass: Class<T>): kotlin.collections.List<T> {
        if (!excelFile.exists()) {
            throw RuntimeException("ExcelTool readExcel error, excelFile is null or not exists.")
        }
        validateExcelPath(excelFile.path)
        try {
            return readExcel(Files.newInputStream(excelFile.toPath()), sheetClass)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    fun <T> readExcel(filePath: String, sheetClass: Class<T>): kotlin.collections.List<T> = readExcel(File(filePath), sheetClass)
    fun <T> readExcel(inputStream: InputStream, consumer: Consumer<T>) {
        try {
            val workbook = WorkbookFactory.create(inputStream)
            workbook.use {
                val parameterizedType = consumer.javaClass.genericInterfaces[0] as ParameterizedType
                @Suppress("UNCHECKED_CAST")
                val sheetClass = parameterizedType.actualTypeArguments[0] as Class<T>
                readSheet(it, sheetClass, null, consumer)
            }
        } catch (e: IOException) {
            throw RuntimeException("ExcelTool readExcel error, ${e.message}", e)
        } catch (e: EncryptedDocumentException) {
            throw RuntimeException("ExcelTool readExcel error, ${e.message}", e)
        }
    }
    fun <T> readExcel(excelFile: File, consumer: Consumer<T>) {
        if (!excelFile.exists()) {
            throw RuntimeException("ExcelTool readExcel error, excelFile is null or not exists.")
        }
        validateExcelPath(excelFile.path)
        try {
            readExcel(Files.newInputStream(excelFile.toPath()), consumer)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
    fun <T> readExcel(filePath: String, consumer: Consumer<T>) {
        readExcel(File(filePath), consumer)
    }

    private fun resolveSheetName(
        sheetClass: Class<*>,
        excelSheetAnno: ExcelSheet?,
    ): String {
        return if (excelSheetAnno != null && excelSheetAnno.name.trim().isNotEmpty()) {
            excelSheetAnno.name.trim()
        } else {
            sheetClass.simpleName
        }
    }

    private fun collectSheetFields(sheetClass: Class<*>): kotlin.collections.List<Field> {
        val fields = ArrayList<Field>()
        for (field in sheetClass.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) {
                continue
            }
            val excelFieldAnno = field.getAnnotation(ExcelField::class.java)
            if (excelFieldAnno != null && excelFieldAnno.ignore) {
                continue
            }
            fields.add(field)
        }
        return fields
    }

    /**
     * 读取导入对象时显式要求无参构造，和旧实现一致，避免半初始化对象混进结果里。
     */
    private fun <T> findDefaultConstructor(sheetClass: Class<T>): Constructor<T> {
        for (constructor in sheetClass.declaredConstructors) {
            if (constructor.parameterCount == 0) {
                @Suppress("UNCHECKED_CAST")
                return constructor as Constructor<T>
            }
        }
        throw RuntimeException("ExcelTool readSheet error, sheetClass[${sheetClass.name}] does not have default constructor.")
    }

    private fun validateExcelPath(filePath: String) {
        val lowerPath = filePath.lowercase()
        if (lowerPath.endsWith(".xls")) {
            throw RuntimeException("ExcelTool not support Excel 2003 (.xls): $filePath")
        }
    }
}
