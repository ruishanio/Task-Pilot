package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskReport
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.util.Date

/**
 * 日志汇总 Mapper。
 */
@Mapper
interface TaskReportMapper {
    fun saveOrUpdate(taskReport: TaskReport): Int

    fun queryLogReport(
        @Param("triggerDayFrom") triggerDayFrom: Date?,
        @Param("triggerDayTo") triggerDayTo: Date?
    ): List<TaskReport>

    fun queryLogReportTotal(): TaskReport?
}
