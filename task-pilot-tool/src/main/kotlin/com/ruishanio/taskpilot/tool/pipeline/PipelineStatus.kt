package com.ruishanio.taskpilot.tool.pipeline

import com.ruishanio.taskpilot.tool.core.StringTool

/**
 * Pipeline 状态枚举，继续保留状态码与中文描述双字段，供持久化与展示层同时使用。
 */
enum class PipelineStatus(val status: String, val desc: String) {
    RUNTIME("RUNTIME", "运行中"),
    PAUSED("PAUSED", "暂停"),
    FAILED("FAILED", "异常");

    companion object {
        fun findByStatus(status: String?): PipelineStatus? {
            if (StringTool.isBlank(status)) {
                return null
            }
            return entries.firstOrNull { it.status == status }
        }
    }
}
