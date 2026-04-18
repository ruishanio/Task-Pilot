package com.ruishanio.taskpilot.core.openapi.model

import java.io.Serializable

/**
 * 任务触发请求。
 */
data class TriggerRequest(
    /**
     * 任务基础信息。
     */
    var jobId: Int = 0,
    /**
     * 任务执行信息。
     */
    var executorHandler: String? = null,
    var executorParams: String? = null,
    var executorBlockStrategy: String? = null,
    var executorTimeout: Int = 0,
    /**
     * 执行日志定位信息。
     */
    var logId: Long = 0,
    var logDateTime: Long = 0,
    /**
     * GLUE 信息。
     */
    var glueType: String? = null,
    var glueSource: String? = null,
    var glueUpdatetime: Long = 0,
    /**
     * 广播分片信息。
     */
    var broadcastIndex: Int = 0,
    var broadcastTotal: Int = 0
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
