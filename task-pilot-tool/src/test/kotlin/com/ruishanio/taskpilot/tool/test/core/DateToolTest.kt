package com.ruishanio.taskpilot.tool.test.core

import com.ruishanio.taskpilot.tool.core.DateTool
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.Date

/**
 * DateTool 日期格式化和时间计算验证。
 */
class DateToolTest {
    @Test
    fun formatTest() {
        val now = Date()
        val dateTimeStr = DateTool.formatDateTime(now)
        logger.info("formatDateTime = {}", dateTimeStr)

        val dateStr = DateTool.formatDate(now)
        logger.info("formatDate = {}", dateStr)

        val formatStr1 = DateTool.format(now, "yyyy-MM-dd HH")
        logger.info("format = {}", formatStr1)
        val formatStr2 = DateTool.format(now, "yyyy-MM")
        logger.info("format = {}", formatStr2)

        logger.info("parseDateTime = {}", DateTool.formatDateTime(DateTool.parseDateTime(dateTimeStr)))
        logger.info("parseDate = {}", DateTool.formatDateTime(DateTool.parseDate(dateStr)))
        logger.info("parse1 = {}", DateTool.formatDateTime(DateTool.parse(formatStr1, "yyyy-MM-dd HH")))
        logger.info("parse2 = {}", DateTool.formatDateTime(DateTool.parse(formatStr2, "yyyy-MM")))
    }

    @Test
    fun addTime() {
        val now = Date()
        logger.info("origin = {}", DateTool.formatDateTime(now))
        logger.info("addYears = {}", DateTool.formatDateTime(DateTool.addYears(now, 1)))
        logger.info("addMonths = {}", DateTool.formatDateTime(DateTool.addMonths(now, 1)))
        logger.info("addDays = {}", DateTool.formatDateTime(DateTool.addDays(now, 1)))
        logger.info("addHours = {}", DateTool.formatDateTime(DateTool.addHours(now, 1)))
        logger.info("addMinutes = {}", DateTool.formatDateTime(DateTool.addMinutes(now, 1)))
        logger.info("addSeconds = {}", DateTool.formatDateTime(DateTool.addSeconds(now, 1)))
    }

    @Test
    fun set() {
        val now = Date()
        logger.info("setYears = {}", DateTool.formatDateTime(DateTool.setYears(now, 2025)))
        logger.info("setMonths = {}", DateTool.formatDateTime(DateTool.setMonths(now, 0)))
        logger.info("setDays = {}", DateTool.formatDateTime(DateTool.setDays(now, 1)))
        logger.info("setHours = {}", DateTool.formatDateTime(DateTool.setHours(now, 1)))
        logger.info("setMinutes = {}", DateTool.formatDateTime(DateTool.setMinutes(now, 1)))
        logger.info("setSeconds = {}", DateTool.formatDateTime(DateTool.setSeconds(now, 1)))
        logger.info("setMilliseconds = {}", DateTool.formatDateTime(DateTool.setMilliseconds(now, 1)))
        logger.info("setStartOfDay = {}", DateTool.formatDateTime(DateTool.setStartOfDay(now)))
    }

    @Test
    fun between() {
        val now = Date()
        logger.info("between 1year = {}", DateTool.betweenYear(DateTool.addYears(now, -1), now))
        logger.info("between 1year = {}", DateTool.betweenMonth(DateTool.addYears(now, -1), now))
        logger.info("between 1year = {}", DateTool.betweenDay(DateTool.addYears(now, -1), now))
        logger.info("between 1h = {}", DateTool.betweenHour(DateTool.addHours(now, -1), now))
        logger.info("between 1h = {}", DateTool.betweenMinute(DateTool.addHours(now, -1), now))
        logger.info("between 1h = {}", DateTool.betweenSecond(DateTool.addHours(now, -1), now))
        logger.info("between 2w = {}", DateTool.betweenWeek(DateTool.addWeeks(now, -2), now))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DateToolTest::class.java)
    }
}
