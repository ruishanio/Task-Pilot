package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskPilotRegistry
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.util.Date

/**
 * 注册中心 Mapper。
 */
@Mapper
interface TaskPilotRegistryMapper {
    fun findDead(@Param("timeout") timeout: Int, @Param("nowTime") nowTime: Date?): List<Int>

    fun removeDead(@Param("ids") ids: List<Int>): Int

    fun findAll(@Param("timeout") timeout: Int, @Param("nowTime") nowTime: Date?): List<TaskPilotRegistry>

    fun registrySaveOrUpdate(
        @Param("registryGroup") registryGroup: String?,
        @Param("registryKey") registryKey: String?,
        @Param("registryValue") registryValue: String?,
        @Param("updateTime") updateTime: Date?
    ): Int

    fun registryDelete(
        @Param("registryGroup") registryGroup: String?,
        @Param("registryKey") registryKey: String?,
        @Param("registryValue") registryValue: String?
    ): Int

    fun removeByRegistryGroupAndKey(
        @Param("registryGroup") registryGroup: String?,
        @Param("registryKey") registryKey: String?
    ): Int
}
