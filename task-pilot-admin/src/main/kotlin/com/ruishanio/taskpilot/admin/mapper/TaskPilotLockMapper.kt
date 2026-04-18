package com.ruishanio.taskpilot.admin.mapper

import org.apache.ibatis.annotations.Mapper

/**
 * 调度锁 Mapper。
 */
@Mapper
interface TaskPilotLockMapper {
    /**
     * 获取调度锁标记。
     */
    fun scheduleLock(): String?
}
