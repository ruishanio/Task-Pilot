package com.ruishanio.taskpilot.tool.id

import com.ruishanio.taskpilot.tool.core.DateTool
import java.util.Date

/**
 * 时间前缀 ID 工具，通过“毫秒时间戳 + 随机尾巴”兼顾可读性与基础去重能力。
 */
object DateIdTool {
    fun getDateId(): String = getDateId(3)
    fun getDateId(suffixLen: Int): String {
        val dateStr = DateTool.format(Date(), "yyyyMMddHHmmssSSS")
        val randomSuffix = RandomIdTool.getDigitId(suffixLen)
        return dateStr + randomSuffix
    }
}
