package com.ruishanio.taskpilot.admin.core.util

import com.ruishanio.taskpilot.admin.scheduler.cron.CronExpression
import com.ruishanio.taskpilot.tool.core.DateTool
import java.text.ParseException
import java.util.Date
import org.junit.jupiter.api.Test

/**
 * 验证 CronExpression 包装层仍能按周表达式计算触发时间。
 */
class CronExpressionTest {
    @Test
    @Throws(ParseException::class)
    fun shouldWriteValueAsString() {
        val cronExpression = CronExpression("0 0 0 ? * 1")
        var lastTriggerTime = Date()
        repeat(5) {
            val nextTriggerTime = requireNotNull(cronExpression.getNextValidTimeAfter(lastTriggerTime))
            println(DateTool.formatDateTime(nextTriggerTime))
            lastTriggerTime = nextTriggerTime
        }
    }
}
