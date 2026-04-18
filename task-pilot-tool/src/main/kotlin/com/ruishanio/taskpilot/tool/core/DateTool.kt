package com.ruishanio.taskpilot.tool.core

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/**
 * 日期工具。
 * 继续同时保留 `Date/Calendar` 和 `java.time` 两套入口，避免现有调用方被迫一次性切换。
 */
object DateTool {
    private val logger = LoggerFactory.getLogger(DateTool::class.java)

    const val DATE_TIME: String = "yyyy-MM-dd HH:mm:ss"
    const val DATE: String = "yyyy-MM-dd"
    const val TIME: String = "HH:mm:ss"
    const val TIME_WITHOUT_SECOND: String = "HH:mm"

    private val dateFormatThreadLocal: ThreadLocal<ConcurrentHashMap<String, DateFormat>> =
        ThreadLocal.withInitial { ConcurrentHashMap() }

    private fun loadDateFormat(pattern: String): DateFormat {
        if (pattern.trim().isEmpty()) {
            throw IllegalArgumentException("pattern cannot be empty.")
        }
        return dateFormatThreadLocal.get().computeIfAbsent(pattern.trim()) {
            SimpleDateFormat(it, Locale.getDefault())
        }
    }
    fun parse(dateString: String, pattern: String): Date {
        return try {
            loadDateFormat(pattern).parse(dateString)
        } catch (e: ParseException) {
            throw RuntimeException("parse error.", e)
        }
    }
    fun parseDateTime(dateString: String): Date = parse(dateString, DATE_TIME)
    fun parseDate(dateString: String): Date = parse(dateString, DATE)
    fun format(date: Date, pattern: String): String = loadDateFormat(pattern).format(date)
    fun formatDate(date: Date): String = format(date, DATE)
    fun formatDateTime(date: Date): String = format(date, DATE_TIME)
    fun parseLocalDateTime(dateString: String, pattern: String): LocalDateTime =
        LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(pattern))
    fun parseLocalDate(dateString: String, pattern: String): LocalDate =
        LocalDate.parse(dateString, DateTimeFormatter.ofPattern(pattern))
    fun parseLocalTime(dateString: String, pattern: String): LocalTime =
        LocalTime.parse(dateString, DateTimeFormatter.ofPattern(pattern))
    fun parseYearMonth(dateString: String, pattern: String): YearMonth =
        YearMonth.parse(dateString, DateTimeFormatter.ofPattern(pattern))

    /**
     * `Temporal` 格式化继续只支持项目里实际用到的四类类型，未知类型直接抛错。
     */
    fun formatTemporal(temporal: Temporal, pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return when (temporal) {
            is LocalDateTime -> temporal.format(formatter)
            is LocalDate -> temporal.format(formatter)
            is LocalTime -> temporal.format(formatter)
            is YearMonth -> temporal.format(formatter)
            else -> throw IllegalArgumentException("Unsupported temporal type: " + temporal.javaClass.name)
        }
    }
    fun addYears(date: Date, amount: Long): Date = plusDate(date) { it.plusYears(amount) }
    fun addMonths(date: Date, amount: Long): Date = plusDate(date) { it.plusMonths(amount) }
    fun addDays(date: Date, amount: Long): Date = plusDate(date) { it.plusDays(amount) }
    fun addHours(date: Date, amount: Long): Date = plusDate(date) { it.plusHours(amount) }
    fun addMinutes(date: Date, amount: Long): Date = plusDate(date) { it.plusMinutes(amount) }
    fun addSeconds(date: Date, amount: Long): Date = plusDate(date) { it.plusSeconds(amount) }
    fun addWeeks(date: Date, amount: Long): Date = plusDate(date) { it.plusWeeks(amount) }
    fun addMilliseconds(date: Date, amount: Long): Date = plusDate(date) { it.plusNanos(amount * 1_000_000) }

    private fun plusDate(date: Date, op: (LocalDateTime) -> LocalDateTime): Date {
        val dateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
        val newDateTime = op(dateTime)
        return Date.from(newDateTime.atZone(ZoneId.systemDefault()).toInstant())
    }
    fun set(date: Date, calendarField: Int, amount: Int): Date {
        val c = Calendar.getInstance()
        c.isLenient = false
        c.time = date
        c.set(calendarField, amount)
        return c.time
    }
    fun setYears(date: Date, amount: Int): Date = set(date, Calendar.YEAR, amount)
    fun setMonths(date: Date, amount: Int): Date = set(date, Calendar.MONTH, amount)
    fun setDays(date: Date, amount: Int): Date = set(date, Calendar.DAY_OF_MONTH, amount)
    fun setHours(date: Date, amount: Int): Date = set(date, Calendar.HOUR_OF_DAY, amount)
    fun setMinutes(date: Date, amount: Int): Date = set(date, Calendar.MINUTE, amount)
    fun setSeconds(date: Date, amount: Int): Date = set(date, Calendar.SECOND, amount)
    fun setMilliseconds(date: Date, amount: Int): Date = set(date, Calendar.MILLISECOND, amount)
    fun setStartOfDay(date: Date): Date {
        val c = Calendar.getInstance()
        c.isLenient = false
        c.time = date
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.time
    }

    const val MILLIS_PER_MS: Long = 1
    const val MILLIS_PER_SECOND: Long = 1000
    const val MILLIS_PER_MINUTE: Long = MILLIS_PER_SECOND * 60
    const val MILLIS_PER_HOUR: Long = MILLIS_PER_MINUTE * 60
    const val MILLIS_PER_DAY: Long = MILLIS_PER_HOUR * 24
    const val MILLIS_PER_WEEK: Long = MILLIS_PER_DAY * 7
    const val MILLIS_PER_MONTH_30: Long = MILLIS_PER_DAY * 30
    const val MILLIS_PER_YEAR_365: Long = MILLIS_PER_DAY * 365

    private fun between(beginDate: Date?, endDate: Date?, calendarField: Int): Long {
        if (beginDate == null || endDate == null) {
            throw IllegalArgumentException("date must not be null")
        }

        val unitMillis =
            when (calendarField) {
                Calendar.YEAR -> MILLIS_PER_YEAR_365
                Calendar.MONTH -> MILLIS_PER_MONTH_30
                Calendar.DAY_OF_MONTH, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_YEAR -> MILLIS_PER_DAY
                Calendar.HOUR, Calendar.HOUR_OF_DAY -> MILLIS_PER_HOUR
                Calendar.MINUTE -> MILLIS_PER_MINUTE
                Calendar.SECOND -> MILLIS_PER_SECOND
                Calendar.MILLISECOND -> MILLIS_PER_MS
                Calendar.WEEK_OF_YEAR, Calendar.WEEK_OF_MONTH -> MILLIS_PER_WEEK
                else -> throw IllegalArgumentException("Unsupported time unit: $calendarField")
            }

        val diffMillis = endDate.time - beginDate.time
        return diffMillis / unitMillis
    }
    fun betweenYear(beginDate: Date?, endDate: Date?): Long = between(beginDate, endDate, Calendar.YEAR)
    fun betweenMonth(beginDate: Date?, endDate: Date?): Long = between(beginDate, endDate, Calendar.MONTH)
    fun betweenDay(beginDate: Date?, endDate: Date?): Long = between(beginDate, endDate, Calendar.DAY_OF_MONTH)
    fun betweenHour(beginDate: Date?, endDate: Date?): Long = between(beginDate, endDate, Calendar.HOUR_OF_DAY)
    fun betweenMinute(beginDate: Date?, endDate: Date?): Long = between(beginDate, endDate, Calendar.MINUTE)
    fun betweenSecond(beginDate: Date?, endDate: Date?): Long = between(beginDate, endDate, Calendar.SECOND)
    fun betweenWeek(beginDate: Date?, endDate: Date?): Long = between(beginDate, endDate, Calendar.WEEK_OF_YEAR)
    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        Objects.requireNonNull(cal1, "cal1")
        Objects.requireNonNull(cal2, "cal2")
        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
