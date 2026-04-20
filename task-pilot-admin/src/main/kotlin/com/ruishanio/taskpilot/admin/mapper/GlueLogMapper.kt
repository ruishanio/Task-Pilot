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

    fun findByJobId(@Param("jobId") jobId: Int): List<GlueLog>

    fun removeOld(@Param("jobId") jobId: Int, @Param("limit") limit: Int): Int

    fun deleteByJobId(@Param("jobId") jobId: Int): Int
}
