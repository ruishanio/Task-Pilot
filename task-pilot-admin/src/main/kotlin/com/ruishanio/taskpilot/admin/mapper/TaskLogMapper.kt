package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskLog
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.util.Date

/**
 * 执行日志 Mapper。
 */
@Mapper
interface TaskLogMapper {
    fun pageList(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("executorId") executorId: Int,
        @Param("taskId") taskId: Int,
        @Param("triggerTimeStart") triggerTimeStart: Date?,
        @Param("triggerTimeEnd") triggerTimeEnd: Date?,
        @Param("logStatus") logStatus: Int
    ): List<TaskLog>

    fun pageListCount(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("executorId") executorId: Int,
        @Param("taskId") taskId: Int,
        @Param("triggerTimeStart") triggerTimeStart: Date?,
        @Param("triggerTimeEnd") triggerTimeEnd: Date?,
        @Param("logStatus") logStatus: Int
    ): Int

    fun load(@Param("id") id: Long): TaskLog?

    fun save(taskLog: TaskLog): Long

    fun updateTriggerInfo(taskLog: TaskLog): Int

    fun updateHandleInfo(taskLog: TaskLog): Int

    fun delete(@Param("taskId") taskId: Int): Int

    fun findLogReport(
        @Param("from") from: Date?,
        @Param("to") to: Date?
    ): Map<String, Any>?

    fun findClearLogIds(
        @Param("executorId") executorId: Int,
        @Param("taskId") taskId: Int,
        @Param("clearBeforeTime") clearBeforeTime: Date?,
        @Param("clearBeforeNum") clearBeforeNum: Int,
        @Param("pagesize") pagesize: Int
    ): List<Long>

    fun clearLog(@Param("logIds") logIds: List<Long>): Int

    fun findFailTaskLogIds(@Param("pagesize") pagesize: Int): List<Long>

    fun updateAlarmStatus(
        @Param("logId") logId: Long,
        @Param("oldAlarmStatus") oldAlarmStatus: Int,
        @Param("newAlarmStatus") newAlarmStatus: Int
    ): Int

    fun findLostTaskIds(@Param("losedTime") losedTime: Date?): List<Long>
}
