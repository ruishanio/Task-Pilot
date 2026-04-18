package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskPilotLogGlue
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * GLUE 日志 Mapper。
 */
@Mapper
interface TaskPilotLogGlueMapper {
    fun save(taskPilotLogGlue: TaskPilotLogGlue): Int

    fun findByJobId(@Param("jobId") jobId: Int): List<TaskPilotLogGlue>

    fun removeOld(@Param("jobId") jobId: Int, @Param("limit") limit: Int): Int

    fun deleteByJobId(@Param("jobId") jobId: Int): Int
}
