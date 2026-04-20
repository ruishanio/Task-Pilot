package com.ruishanio.taskpilot.core.openapi.model

import com.ruishanio.taskpilot.core.enums.ExecutorBlockStrategyEnum
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
    /**
     * 当同一任务线程已在执行时，执行器如何处理本次触发请求。
     */
    var executorBlockStrategy: ExecutorBlockStrategyEnum? = null,
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
    var glueUpdateTime: Long = 0,
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
