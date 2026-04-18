package com.ruishanio.taskpilot.tool.crypto

import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Base64 工具，同时提供标准编码与 URL 安全编码，覆盖 JWT、URL 参数和通用文本场景。
 */
object Base64Tool {
    fun encodeUrlSafe(data: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(data.toByteArray(StandardCharsets.UTF_8))
    fun encodeUrlSafe(data: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(data)
    fun decodeUrlSafe(data: String): String =
        String(Base64.getUrlDecoder().decode(data), StandardCharsets.UTF_8)
    fun decodeUrlSafeToBytes(data: String): ByteArray = Base64.getUrlDecoder().decode(data)
    fun encodeStandard(data: String): String =
        Base64.getEncoder().encodeToString(data.toByteArray(StandardCharsets.UTF_8))
    fun encodeStandard(data: ByteArray): String = Base64.getEncoder().encodeToString(data)
    fun decodeStandard(data: String): String =
        String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8)
    fun decodeStandardToBytes(data: String): ByteArray = Base64.getDecoder().decode(data)
}
