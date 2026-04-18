package com.ruishanio.taskpilot.tool.crypto

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * SHA-256 工具，异常信息与入参格式保持稳定，便于排查线上摘要计算问题。
 */
object Sha256Tool {
    private const val SHA_256_ALGORITHM_NAME = "SHA-256"
    fun sha256(input: String): String = sha256(input.toByteArray(StandardCharsets.UTF_8), null)
    fun sha256(input: String, salt: String?): String {
        val saltBytes = salt?.toByteArray(StandardCharsets.UTF_8)
        return sha256(input.toByteArray(StandardCharsets.UTF_8), saltBytes)
    }
    fun sha256(input: ByteArray, salt: ByteArray?): String =
        try {
            val messageDigest = MessageDigest.getInstance(SHA_256_ALGORITHM_NAME)
            if (salt != null) {
                messageDigest.update(salt)
            }
            HexTool.byteToHex(messageDigest.digest(input))!!
        } catch (e: Exception) {
            throw IllegalStateException("SHA256Tool#sha256 error, input:${input.contentToString()}", e)
        }
}
