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
        @Param("executorId") executorId: Int,
        @Param("triggerStatus") triggerStatus: Int,
        @Param("taskName") taskName: String?,
        @Param("taskDesc") taskDesc: String?,
        @Param("executorHandler") executorHandler: String?,
        @Param("author") author: String?
    ): List<TaskInfo>

    fun pageListCount(
        @Param("offset") offset: Int,
        @Param("pagesize") pagesize: Int,
        @Param("executorId") executorId: Int,
        @Param("triggerStatus") triggerStatus: Int,
        @Param("taskName") taskName: String?,
        @Param("taskDesc") taskDesc: String?,
        @Param("executorHandler") executorHandler: String?,
        @Param("author") author: String?
    ): Int

    fun save(info: TaskInfo): Int

    fun loadById(@Param("id") id: Int): TaskInfo?

    /**
     * 按执行器和任务名称查询任务。
     *
     * 任务名称只要求在同一执行器下唯一，因此新增、编辑和自动同步都需要带上执行器维度查询。
     */
    fun loadByExecutorIdAndTaskName(
        @Param("executorId") executorId: Int,
        @Param("taskName") taskName: String?
    ): TaskInfo?

    fun update(taskInfo: TaskInfo): Int

    fun delete(@Param("id") id: Long): Int

    fun getTasksByExecutorId(@Param("executorId") executorId: Int): List<TaskInfo>

    fun findAllCount(): Int

    /**
     * 查询待调度任务，仅返回启用状态的记录。
     */
    fun scheduleTaskQuery(
        @Param("maxNextTime") maxNextTime: Long,
        @Param("pagesize") pagesize: Int
    ): List<TaskInfo>

    /**
     * 更新调度时间窗口，仅允许修改启用中的任务。
     */
    fun scheduleUpdate(taskInfo: TaskInfo): Int
}
