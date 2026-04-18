package com.ruishanio.taskpilot.tool.excel.annotation

import org.apache.poi.ss.usermodel.HorizontalAlignment

/**
 * Excel 字段映射注解。
 * 字段名和默认值保持不变，避免导入导出时的反射读取规则漂移。
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@java.lang.annotation.Inherited
annotation class ExcelField(
    val name: String = "",
    val width: Int = 0,
    val align: HorizontalAlignment = HorizontalAlignment.LEFT,
    val dateformat: String = "yyyy-MM-dd HH:mm:ss",
    val ignore: Boolean = false
)
