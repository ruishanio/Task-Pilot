package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskPilotUser
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * 管理端用户 Mapper。
 */
@Mapper
interface TaskPilotUserMapper {
    fun pageList(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("username") username: String?,
        @Param("role") role: Int
    ): List<TaskPilotUser>

    fun pageListCount(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("username") username: String?,
        @Param("role") role: Int
    ): Int

    fun loadByUserName(@Param("username") username: String?): TaskPilotUser?

    fun loadById(@Param("id") id: Int): TaskPilotUser?

    fun save(taskPilotUser: TaskPilotUser): Int

    fun update(taskPilotUser: TaskPilotUser): Int

    fun delete(@Param("id") id: Int): Int

    fun updateToken(@Param("id") id: Int, @Param("token") token: String?): Int
}
