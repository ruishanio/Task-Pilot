package com.ruishanio.taskpilot.tool.excel.util

import com.ruishanio.taskpilot.tool.excel.annotation.ExcelField
import java.lang.reflect.Field
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Excel 字段转换工具。
 * 继续围绕反射字段做字符串和目标类型互转，不引入额外转换器注册机制。
 */
object ExcelFieldUtil {
    fun parseByte(value: String): Byte {
        return try {
            value.normalizeExcelValue().toByte()
        } catch (e: NumberFormatException) {
            throw RuntimeException("parseByte but input illegal input=$value", e)
        }
    }
    fun parseBoolean(value: String): Boolean {
        val normalizedValue = value.normalizeExcelValue()
        return when {
            "true".equals(normalizedValue, ignoreCase = true) -> true
            "false".equals(normalizedValue, ignoreCase = true) -> false
            else -> throw RuntimeException("parseBoolean but input illegal input=$value")
        }
    }
    fun parseInt(value: String): Int {
        return try {
            Integer.valueOf(value.normalizeExcelValue())
        } catch (e: NumberFormatException) {
            throw RuntimeException("parseInt but input illegal input=$value", e)
        }
    }
    fun parseShort(value: String): Short {
        return try {
            value.normalizeExcelValue().toShort()
        } catch (e: NumberFormatException) {
            throw RuntimeException("parseShort but input illegal input=$value", e)
        }
    }
    fun parseLong(value: String): Long {
        return try {
            value.normalizeExcelValue().toLong()
        } catch (e: NumberFormatException) {
            throw RuntimeException("parseLong but input illegal input=$value", e)
        }
    }
    fun parseFloat(value: String): Float {
        return try {
            value.normalizeExcelValue().toFloat()
        } catch (e: NumberFormatException) {
            throw RuntimeException("parseFloat but input illegal input=$value", e)
        }
    }
    fun parseDouble(value: String): Double {
        return try {
            value.normalizeExcelValue().toDouble()
        } catch (e: NumberFormatException) {
            throw RuntimeException("parseDouble but input illegal input=$value", e)
        }
    }
    fun parseDate(value: String, excelField: ExcelField?): Date {
        return try {
            val datePattern = excelField?.dateformat ?: "yyyy-MM-dd HH:mm:ss"
            SimpleDateFormat(datePattern).parse(value)
        } catch (e: ParseException) {
            throw RuntimeException("parseDate but input illegal input=$value", e)
        }
    }

    /**
     * 参数解析只支持工具历史上约定的基础类型和枚举，避免在 Excel 导入时引入过度隐式转换。
     */
    fun parseValue(field: Field, value: String?): Any? {
        val fieldType = field.type
        val excelField = field.getAnnotation(ExcelField::class.java)
        if (value == null || value.trim().isEmpty()) {
            return null
        }
        val normalizedValue = value.trim()

        return when {
            fieldType == Boolean::class.javaObjectType || fieldType == Boolean::class.javaPrimitiveType -> parseBoolean(normalizedValue)
            fieldType == String::class.java -> normalizedValue
            fieldType == Short::class.javaObjectType || fieldType == Short::class.javaPrimitiveType -> parseShort(normalizedValue)
            fieldType == Int::class.javaObjectType || fieldType == Int::class.javaPrimitiveType -> parseInt(normalizedValue)
            fieldType == Long::class.javaObjectType || fieldType == Long::class.javaPrimitiveType -> parseLong(normalizedValue)
            fieldType == Float::class.javaObjectType || fieldType == Float::class.javaPrimitiveType -> parseFloat(normalizedValue)
            fieldType == Double::class.javaObjectType || fieldType == Double::class.javaPrimitiveType -> parseDouble(normalizedValue)
            fieldType == Date::class.java -> parseDate(normalizedValue, excelField)
            fieldType.isEnum -> parseEnum(fieldType, normalizedValue)
            else -> throw RuntimeException("request illeagal type, type must be Integer not int Long not long etc, type=$fieldType")
        }
    }
    fun formatValue(field: Field, value: Any?): String? {
        val fieldType = field.type
        val excelField = field.getAnnotation(ExcelField::class.java)
        if (value == null) {
            return null
        }

        return when {
            fieldType == Boolean::class.javaObjectType || fieldType == Boolean::class.javaPrimitiveType -> value.toString()
            fieldType == String::class.java -> value.toString()
            fieldType == Short::class.javaObjectType || fieldType == Short::class.javaPrimitiveType -> value.toString()
            fieldType == Int::class.javaObjectType || fieldType == Int::class.javaPrimitiveType -> value.toString()
            fieldType == Long::class.javaObjectType || fieldType == Long::class.javaPrimitiveType -> value.toString()
            fieldType == Float::class.javaObjectType || fieldType == Float::class.javaPrimitiveType -> value.toString()
            fieldType == Double::class.javaObjectType || fieldType == Double::class.javaPrimitiveType -> value.toString()
            fieldType == Date::class.java -> {
                val datePattern = excelField?.dateformat ?: "yyyy-MM-dd HH:mm:ss"
                SimpleDateFormat(datePattern).format(value)
            }
            fieldType.isEnum -> (value as Enum<*>).name
            else -> throw RuntimeException("request illeagal type, type must be Integer not int Long not long etc, type=$fieldType")
        }
    }

    /**
     * 继续只清理 Excel 里常见的全角空格，不额外吞掉普通中间空白，避免改坏值本身。
     */
    private fun String.normalizeExcelValue(): String = replace("　", "")

    private fun parseEnum(
        fieldType: Class<*>,
        value: String,
    ): Any {
        val constants = fieldType.enumConstants ?: throw RuntimeException("enum constants not found, type=$fieldType")
        for (constant in constants) {
            if ((constant as Enum<*>).name == value) {
                return constant
            }
        }
        throw IllegalArgumentException("No enum constant ${fieldType.name}.$value")
    }
}
