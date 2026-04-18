package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskPilotLog
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.util.Date

/**
 * 执行日志 Mapper。
 */
@Mapper
interface TaskPilotLogMapper {
    fun pageList(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("jobGroup") jobGroup: Int,
        @Param("jobId") jobId: Int,
        @Param("triggerTimeStart") triggerTimeStart: Date?,
        @Param("triggerTimeEnd") triggerTimeEnd: Date?,
        @Param("logStatus") logStatus: Int
    ): List<TaskPilotLog>

    fun pageListCount(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("jobGroup") jobGroup: Int,
        @Param("jobId") jobId: Int,
        @Param("triggerTimeStart") triggerTimeStart: Date?,
        @Param("triggerTimeEnd") triggerTimeEnd: Date?,
        @Param("logStatus") logStatus: Int
    ): Int

    fun load(@Param("id") id: Long): TaskPilotLog?

    fun save(taskPilotLog: TaskPilotLog): Long

    fun updateTriggerInfo(taskPilotLog: TaskPilotLog): Int

    fun updateHandleInfo(taskPilotLog: TaskPilotLog): Int

    fun delete(@Param("jobId") jobId: Int): Int

    fun findLogReport(
        @Param("from") from: Date?,
        @Param("to") to: Date?
    ): Map<String, Any>?

    fun findClearLogIds(
        @Param("jobGroup") jobGroup: Int,
        @Param("jobId") jobId: Int,
        @Param("clearBeforeTime") clearBeforeTime: Date?,
        @Param("clearBeforeNum") clearBeforeNum: Int,
        @Param("pagesize") pagesize: Int
    ): List<Long>

    fun clearLog(@Param("logIds") logIds: List<Long>): Int

    fun findFailJobLogIds(@Param("pagesize") pagesize: Int): List<Long>

    fun updateAlarmStatus(
        @Param("logId") logId: Long,
        @Param("oldAlarmStatus") oldAlarmStatus: Int,
        @Param("newAlarmStatus") newAlarmStatus: Int
    ): Int

    fun findLostJobIds(@Param("losedTime") losedTime: Date?): List<Long>
}
