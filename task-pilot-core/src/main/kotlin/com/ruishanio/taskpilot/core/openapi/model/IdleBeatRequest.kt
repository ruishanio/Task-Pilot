package com.ruishanio.taskpilot.core.openapi.model

import java.io.Serializable

/**
 * 空闲检查请求。
 */
data class IdleBeatRequest(
    var jobId: Int = 0
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
