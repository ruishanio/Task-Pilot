package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.GlueLog
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * GLUE 日志 Mapper。
 */
@Mapper
interface GlueLogMapper {
    fun save(glueLog: GlueLog): Int

    fun findByTaskId(@Param("taskId") taskId: Int): List<GlueLog>

    fun removeOld(@Param("taskId") taskId: Int, @Param("limit") limit: Int): Int

    fun deleteByTaskId(@Param("taskId") taskId: Int): Int
}
