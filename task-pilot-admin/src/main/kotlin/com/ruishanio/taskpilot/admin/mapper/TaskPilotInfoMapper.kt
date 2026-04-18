package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskPilotInfo
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * 任务定义 Mapper。
 */
@Mapper
interface TaskPilotInfoMapper {
    fun pageList(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("jobGroup") jobGroup: Int,
        @Param("triggerStatus") triggerStatus: Int,
        @Param("jobDesc") jobDesc: String?,
        @Param("executorHandler") executorHandler: String?,
        @Param("author") author: String?
    ): List<TaskPilotInfo>

    fun pageListCount(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("jobGroup") jobGroup: Int,
        @Param("triggerStatus") triggerStatus: Int,
        @Param("jobDesc") jobDesc: String?,
        @Param("executorHandler") executorHandler: String?,
        @Param("author") author: String?
    ): Int

    fun save(info: TaskPilotInfo): Int

    fun loadById(@Param("id") id: Int): TaskPilotInfo?

    /**
     * 按执行器分组与 handler 名称查询任务。
     *
     * 自动注册会基于该结果判断是新增任务还是同步更新已有任务。
     */
    fun loadByGroupAndExecutorHandler(
        @Param("jobGroup") jobGroup: Int,
        @Param("executorHandler") executorHandler: String?
    ): TaskPilotInfo?

    fun update(taskPilotInfo: TaskPilotInfo): Int

    fun delete(@Param("id") id: Long): Int

    fun getJobsByGroup(@Param("jobGroup") jobGroup: Int): List<TaskPilotInfo>

    fun findAllCount(): Int

    /**
     * 查询待调度任务，仅返回启用状态的记录。
     */
    fun scheduleJobQuery(
        @Param("maxNextTime") maxNextTime: Long,
        @Param("pagesize") pagesize: Int
    ): List<TaskPilotInfo>

    /**
     * 更新调度时间窗口，仅允许修改启用中的任务。
     */
    fun scheduleUpdate(taskPilotInfo: TaskPilotInfo): Int
}
