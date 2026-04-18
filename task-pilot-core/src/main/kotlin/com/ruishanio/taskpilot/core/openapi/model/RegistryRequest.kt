package com.ruishanio.taskpilot.core.openapi.model

import java.io.Serializable

/**
 * 注册中心请求。
 */
data class RegistryRequest(
    var registryGroup: String? = null,
    var registryKey: String? = null,
    var registryValue: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 42L
    }
}
