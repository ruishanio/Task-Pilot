package com.ruishanio.taskpilot.admin.mapper

import com.ruishanio.taskpilot.admin.model.TaskInfo
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

/**
 * 任务定义 Mapper。
 */
@Mapper
interface TaskInfoMapper {
    fun pageList(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("jobGroup") jobGroup: Int,
        @Param("triggerStatus") triggerStatus: Int,
        @Param("taskName") taskName: String?,
        @Param("jobDesc") jobDesc: String?,
        @Param("executorHandler") executorHandler: String?,
        @Param("author") author: String?
    ): List<TaskInfo>

    fun pageListCount(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("jobGroup") jobGroup: Int,
        @Param("triggerStatus") triggerStatus: Int,
        @Param("taskName") taskName: String?,
        @Param("jobDesc") jobDesc: String?,
        @Param("executorHandler") executorHandler: String?,
        @Param("author") author: String?
    ): Int

    fun save(info: TaskInfo): Int

    fun loadById(@Param("id") id: Int): TaskInfo?

    /**
     * 按任务唯一名称查询任务。
     *
     * 管理端新增和编辑都依赖该查询提前返回可读错误，避免直接把唯一约束异常暴露给用户。
     */
    fun loadByTaskName(@Param("taskName") taskName: String?): TaskInfo?

    fun update(taskInfo: TaskInfo): Int

    fun delete(@Param("id") id: Long): Int

    fun getJobsByGroup(@Param("jobGroup") jobGroup: Int): List<TaskInfo>

    fun findAllCount(): Int

    /**
     * 查询待调度任务，仅返回启用状态的记录。
     */
    fun scheduleJobQuery(
        @Param("maxNextTime") maxNextTime: Long,
        @Param("pagesize") pagesize: Int
    ): List<TaskInfo>

    /**
     * 更新调度时间窗口，仅允许修改启用中的任务。
     */
    fun scheduleUpdate(taskInfo: TaskInfo): Int
}
