package com.ruishanio.taskpilot.tool.id

import com.ruishanio.taskpilot.tool.core.StringTool
import java.util.Random

/**
 * 随机 ID 工具，继续沿用字符集 + 指定长度的生成方式，避免上层展示和存储长度漂移。
 */
object RandomIdTool {
    private const val DEFAULT_LENGTH = 20
    private const val DIGITS = "0123456789"
    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val SPECIAL_CHARS = "!@#$%^&*()-_=+[]{}|;:,.<>?/"
    fun getDigitId(): String = getDigitId(DEFAULT_LENGTH)
    fun getDigitId(length: Int): String = getRandomId(DIGITS, length)
    fun getLowercaseId(): String = getLowercaseId(DEFAULT_LENGTH)
    fun getLowercaseId(length: Int): String = getRandomId(LOWERCASE, length)
    fun getUppercaseId(): String = getUppercaseId(DEFAULT_LENGTH)
    fun getUppercaseId(length: Int): String = getRandomId(UPPERCASE, length)
    fun getAlphaNumeric(): String = getAlphaNumeric(DEFAULT_LENGTH)
    fun getAlphaNumeric(length: Int): String = getRandomId(DIGITS + LOWERCASE + UPPERCASE, length)
    fun getAlphaNumericWithSpecial(): String = getAlphaNumericWithSpecial(DEFAULT_LENGTH)
    fun getAlphaNumericWithSpecial(length: Int): String = getRandomId(DIGITS + LOWERCASE + UPPERCASE + SPECIAL_CHARS, length)

    /**
     * 对长度和字符集做前置校验，避免生成阶段出现空字符集或异常长度导致的隐式错误。
     */
    fun getRandomId(characters: String?, length: Int): String {
        if (length !in 1..1000) {
            throw IllegalArgumentException("random length must be between 1 and 1000.")
        }
        if (StringTool.isBlank(characters)) {
            throw IllegalArgumentException("random characters can't be empty.")
        }

        val random = Random()
        val builder = StringBuilder(length)
        repeat(length) {
            val index = random.nextInt(characters!!.length)
            builder.append(characters[index])
        }
        return builder.toString()
    }
}
