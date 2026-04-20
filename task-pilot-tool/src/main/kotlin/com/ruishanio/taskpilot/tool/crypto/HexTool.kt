package com.ruishanio.taskpilot.tool.crypto

import java.nio.charset.StandardCharsets

/**
 * Hex 编解码工具，继续保留旧版输出格式与异常文案，避免已有断言或日志匹配失效。
 */
object HexTool {
    private val HEX = byteArrayOf(
        '0'.code.toByte(), '1'.code.toByte(), '2'.code.toByte(), '3'.code.toByte(),
        '4'.code.toByte(), '5'.code.toByte(), '6'.code.toByte(), '7'.code.toByte(),
        '8'.code.toByte(), '9'.code.toByte(), 'a'.code.toByte(), 'b'.code.toByte(),
        'c'.code.toByte(), 'd'.code.toByte(), 'e'.code.toByte(), 'f'.code.toByte()
    )
    private val HEX_CHARS = "0123456789abcdef".toCharArray()

    /**
     * 直接按 ASCII 区间解码，避免手写映射表在大小写字符边界上出现错位。
     */
    fun getDec(index: Int): Int =
        when (index) {
            in '0'.code..'9'.code -> index - '0'.code
            in 'A'.code..'F'.code -> index - 'A'.code + 10
            in 'a'.code..'f'.code -> index - 'a'.code + 10
            else -> -1
        }

    fun getHex(index: Int): Byte = HEX[index]

    fun toHexString(c: Char): String {
        val builder = StringBuilder(4)
        builder.append(HEX_CHARS[(c.code and 0xf000) shr 12])
        builder.append(HEX_CHARS[(c.code and 0x0f00) shr 8])
        builder.append(HEX_CHARS[(c.code and 0x00f0) shr 4])
        builder.append(HEX_CHARS[c.code and 0x000f])
        return builder.toString()
    }
    fun toHex(input: String): String = byteToHex(input.toByteArray(StandardCharsets.UTF_8))!!

    fun byteToHex(bytes: ByteArray?): String? {
        if (bytes == null) {
            return null
        }
        val builder = StringBuilder(bytes.size shl 1)
        for (item in bytes) {
            builder.append(HEX_CHARS[(item.toInt() and 0xf0) shr 4]).append(HEX_CHARS[item.toInt() and 0x0f])
        }
        return builder.toString()
    }

    fun fromHex(input: String): String = String(hexToByte(input)!!, StandardCharsets.UTF_8)

    fun hexToByte(input: String?): ByteArray? {
        if (input == null) {
            return null
        }
        if ((input.length and 1) == 1) {
            throw IllegalArgumentException("Odd number of characters")
        }

        val result = ByteArray(input.length shr 1)
        for (index in result.indices) {
            val upperNibble = getDec(input[index * 2].code)
            val lowerNibble = getDec(input[index * 2 + 1].code)
            if (upperNibble < 0 || lowerNibble < 0) {
                throw IllegalArgumentException("None hex character")
            }
            result[index] = ((upperNibble shl 4) + lowerNibble).toByte()
        }
        return result
    }
}
