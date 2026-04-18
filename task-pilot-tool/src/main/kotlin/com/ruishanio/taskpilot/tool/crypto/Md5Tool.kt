package com.ruishanio.taskpilot.tool.crypto

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * MD5 工具，保持字符串与字节数组两套入口，兼容旧调用链的盐值追加方式。
 */
object Md5Tool {
    private const val MD5_ALGORITHM_NAME = "MD5"
    fun md5(input: String): String = md5(input.toByteArray(StandardCharsets.UTF_8), null)
    fun md5(input: String, salt: String?): String {
        val saltBytes = salt?.toByteArray(StandardCharsets.UTF_8)
        return md5(input.toByteArray(StandardCharsets.UTF_8), saltBytes)
    }
    fun md5(input: ByteArray, salt: ByteArray?): String =
        try {
            val messageDigest = MessageDigest.getInstance(MD5_ALGORITHM_NAME)
            if (salt != null) {
                messageDigest.update(salt)
            }
            HexTool.byteToHex(messageDigest.digest(input))!!
        } catch (e: Exception) {
            throw IllegalStateException("Md5Tool#md5 error, input:${input.contentToString()}", e)
        }
}
