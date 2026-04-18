package com.ruishanio.taskpilot.admin.scheduler.cron

import java.io.Serializable
import java.text.ParseException
import java.util.Date
import java.util.TimeZone
import org.quartz.CronExpression as QuartzCronExpression

/**
 * Kotlin 侧保留原有 `CronExpression` API 形态，但内部直接委托给 Quartz 原生实现。
 * 这样既能完成源码 Kotlin 化，也能避免继续维护项目内 1600+ 行的 Cron 解析算法副本。
 */
class CronExpression : Serializable, Cloneable {
    private val delegate: QuartzCronExpression

    /**
     * 构造时沿用旧实现的空值保护与解析异常语义，避免调度配置校验行为漂移。
     */
    @Throws(ParseException::class)
    constructor(cronExpression: String?) {
        val expressionValue = requireNotNull(cronExpression) { "cronExpression cannot be null" }
        delegate = QuartzCronExpression(expressionValue)
    }

    constructor(expression: CronExpression?) : this(requireNotNull(expression) { "expression cannot be null" }.getCronExpression()) {
        timeZone = expression.timeZone
    }

    fun isSatisfiedBy(date: Date): Boolean = delegate.isSatisfiedBy(date)

    fun getNextValidTimeAfter(date: Date): Date? = delegate.getNextValidTimeAfter(date)

    fun getNextInvalidTimeAfter(date: Date): Date? = delegate.getNextInvalidTimeAfter(date)

    var timeZone: TimeZone
        get() = delegate.timeZone
        set(value) {
            delegate.timeZone = value
        }

    fun getCronExpression(): String = delegate.cronExpression

    fun getExpressionSummary(): String = delegate.expressionSummary

    fun getTimeAfter(afterTime: Date): Date? = delegate.getTimeAfter(afterTime)

    fun getTimeBefore(endTime: Date): Date? = delegate.getTimeBefore(endTime)

    fun getFinalFireTime(): Date? = delegate.finalFireTime

    override fun toString(): String = delegate.toString()

    public override fun clone(): Any = CronExpression(this)

    companion object {
        private const val serialVersionUID = 12423409423L
        val MAX_YEAR: Int = QuartzCronExpression.MAX_YEAR
        fun isValidExpression(cronExpression: String?): Boolean {
            val expressionValue = requireNotNull(cronExpression) { "cronExpression cannot be null" }
            return QuartzCronExpression.isValidExpression(expressionValue)
        }
        @Throws(ParseException::class)
        fun validateExpression(cronExpression: String?) {
            val expressionValue = requireNotNull(cronExpression) { "cronExpression cannot be null" }
            QuartzCronExpression.validateExpression(expressionValue)
        }
    }
}
