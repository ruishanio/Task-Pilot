package com.ruishanio.taskpilot.core.openapi.model

import java.io.Serializable

/**
 * 终止执行请求。
 */
data class KillRequest(
    var jobId: Int = 0
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
