package com.ruishanio.taskpilot.tool.emoji.model

import com.ruishanio.taskpilot.tool.emoji.fitzpatrick.Fitzpatrick

/**
 * Unicode Emoji 匹配结果。
 */
class UnicodeCandidate(
    val emoji: Emoji,
    fitzpatrick: String?,
    private val startIndex: Int,
) {
    val fitzpatrick: Fitzpatrick? = Fitzpatrick.fitzpatrickFromUnicode(fitzpatrick)

    fun hasFitzpatrick(): Boolean = fitzpatrick != null

    fun getFitzpatrickType(): String = if (hasFitzpatrick()) fitzpatrick!!.name.lowercase() else ""

    fun getFitzpatrickUnicode(): String = if (hasFitzpatrick()) fitzpatrick!!.unicode else ""

    fun getEmojiStartIndex(): Int = startIndex

    fun getEmojiEndIndex(): Int = startIndex + emoji.getUnicode().length

    fun getFitzpatrickEndIndex(): Int = getEmojiEndIndex() + if (fitzpatrick != null) 2 else 0
}
