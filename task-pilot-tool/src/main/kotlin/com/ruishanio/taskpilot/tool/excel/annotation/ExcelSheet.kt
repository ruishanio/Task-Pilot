package com.ruishanio.taskpilot.tool.excel.annotation

import org.apache.poi.ss.usermodel.IndexedColors

/**
 * Excel Sheet 级别配置注解。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@java.lang.annotation.Inherited
annotation class ExcelSheet(
    val name: String = "",
    val headColor: IndexedColors = IndexedColors.LIGHT_GREEN
)
