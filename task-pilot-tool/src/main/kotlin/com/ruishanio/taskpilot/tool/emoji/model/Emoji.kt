package com.ruishanio.taskpilot.tool.emoji.model

import com.ruishanio.taskpilot.tool.emoji.exception.TaskPilotEmojiException
import com.ruishanio.taskpilot.tool.emoji.fitzpatrick.Fitzpatrick
import java.util.Collections

/**
 * Emoji 模型。
 * 继续把 HTML 十进制和十六进制表示预计算出来，避免每次编码都重新遍历 code point。
 */
class Emoji(
    private val unicode: String,
    aliases: kotlin.collections.List<String>,
    tags: kotlin.collections.List<String>,
    private val supportsFitzpatrick: Boolean,
) {
    private val aliases: kotlin.collections.List<String> = Collections.unmodifiableList(aliases)
    private val tags: kotlin.collections.List<String> = Collections.unmodifiableList(tags)
    private val htmlDec: String
    private val htmlHex: String

    init {
        val stringLength = unicode.length
        val pointCodes = arrayOfNulls<String>(stringLength)
        val pointCodesHex = arrayOfNulls<String>(stringLength)

        var count = 0
        var offset = 0
        while (offset < stringLength) {
            val codePoint = unicode.codePointAt(offset)
            pointCodes[count] = String.format("&#%d;", codePoint)
            pointCodesHex[count] = String.format("&#x%x;", codePoint)
            count++
            offset += Character.charCount(codePoint)
        }
        htmlDec = stringJoin(pointCodes, count)
        htmlHex = stringJoin(pointCodesHex, count)
    }

    private fun stringJoin(
        array: Array<String?>,
        count: Int,
    ): String {
        val stringBuilder = StringBuilder()
        for (i in 0 until count) {
            stringBuilder.append(array[i])
        }
        return stringBuilder.toString()
    }

    fun supportsFitzpatrick(): Boolean = supportsFitzpatrick

    fun getAliases(): kotlin.collections.List<String> = aliases

    fun getTags(): kotlin.collections.List<String> = tags

    fun getUnicode(): String = unicode

    fun getHtmlDecimal(): String = htmlDec

    fun getHtmlHexadecimal(): String = htmlHex

    fun getUnicode(fitzpatrick: Fitzpatrick?): String {
        if (!supportsFitzpatrick()) {
            throw TaskPilotEmojiException("cannot get unicode with given fitzpatrick modifier, the emoji doesn't support fitzpatrick.")
        } else if (fitzpatrick == null) {
            return getUnicode()
        }
        return getUnicode() + fitzpatrick.unicode
    }

    override fun equals(other: Any?): Boolean {
        return other is Emoji && other.getUnicode() == getUnicode()
    }

    override fun hashCode(): Int = unicode.hashCode()
}
