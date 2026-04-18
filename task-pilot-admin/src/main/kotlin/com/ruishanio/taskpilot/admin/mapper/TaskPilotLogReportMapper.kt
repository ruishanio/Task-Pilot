package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskPilotLogReport
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.util.Date

/**
 * 日志汇总 Mapper。
 */
@Mapper
interface TaskPilotLogReportMapper {
    fun saveOrUpdate(taskPilotLogReport: TaskPilotLogReport): Int

    fun queryLogReport(
        @Param("triggerDayFrom") triggerDayFrom: Date?,
        @Param("triggerDayTo") triggerDayTo: Date?
    ): List<TaskPilotLogReport>

    fun queryLogReportTotal(): TaskPilotLogReport?
}
