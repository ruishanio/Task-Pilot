package com.ruishanio.taskpilot.tool.emoji

import com.ruishanio.taskpilot.tool.emoji.encode.EmojiEncode
import com.ruishanio.taskpilot.tool.emoji.factory.EmojiFactory
import com.ruishanio.taskpilot.tool.emoji.fitzpatrick.FitzpatrickAction
import com.ruishanio.taskpilot.tool.emoji.model.Emoji
import com.ruishanio.taskpilot.tool.emoji.transformer.EmojiTransformer

/**
 * Emoji 工具。
 * 聚合编码、解码、删除和查找四类能力，避免业务层直接感知底层 emoji 解析细节。
 */
object EmojiTool {
    private fun encodeUnicode(
        input: String,
        transformer: EmojiTransformer,
        fitzpatrickAction: FitzpatrickAction?,
    ): String {
        var prev = 0
        val stringBuilder = StringBuilder()
        val replacements = EmojiFactory.getUnicodeCandidates(input)
        for (candidate in replacements) {
            stringBuilder.append(input.substring(prev, candidate.getEmojiStartIndex()))
            stringBuilder.append(transformer.transform(candidate, fitzpatrickAction))
            prev = candidate.getFitzpatrickEndIndex()
        }
        return stringBuilder.append(input.substring(prev)).toString()
    }
    fun encodeUnicode(
        input: String,
        emojiEncode: EmojiEncode?,
        fitzpatrickAction: FitzpatrickAction?,
    ): String {
        val finalEmojiEncode = emojiEncode ?: EmojiEncode.ALIASES
        val finalAction = fitzpatrickAction ?: FitzpatrickAction.PARSE
        return encodeUnicode(input, finalEmojiEncode.emojiTransformer, finalAction)
    }
    fun encodeUnicode(
        input: String,
        emojiEncode: EmojiEncode?,
    ): String = encodeUnicode(input, emojiEncode, FitzpatrickAction.PARSE)
    fun encodeUnicode(input: String): String = encodeUnicode(input, EmojiEncode.ALIASES)
    fun decodeToUnicode(
        input: String,
        emojiEncode: EmojiEncode?,
    ): String {
        var result = input

        if (emojiEncode == null || emojiEncode == EmojiEncode.ALIASES) {
            val candidates = EmojiFactory.getAliasCandidates(input)
            for (candidate in candidates) {
                val emoji = EmojiFactory.getForAlias(candidate.alias)
                if (emoji != null) {
                    if (emoji.supportsFitzpatrick() || (!emoji.supportsFitzpatrick() && candidate.fitzpatrick == null)) {
                        var replacement = emoji.getUnicode()
                        val fitzpatrick = candidate.fitzpatrick
                        if (fitzpatrick != null) {
                            replacement += fitzpatrick.unicode
                        }
                        result = result.replace(":" + candidate.fullString + ":", replacement)
                    }
                }
            }
        }

        for (emoji in EmojiFactory.getAll()) {
            if (emojiEncode == null || emojiEncode == EmojiEncode.HTML_DECIMAL) {
                result = result.replace(emoji.getHtmlDecimal(), emoji.getUnicode())
            }
            if (emojiEncode == null || emojiEncode == EmojiEncode.HTML_HEX_DECIMAL) {
                result = result.replace(emoji.getHtmlHexadecimal(), emoji.getUnicode())
            }
        }
        return result
    }
    fun decodeToUnicode(input: String): String = decodeToUnicode(input, null)

    /**
     * 删除 emoji 时仍通过统一的 unicode 替换链处理，避免单独维护一套扫描逻辑。
     */
    fun removeEmojis(
        input: String,
        emojisToRemove: Collection<Emoji>?,
        emojisToKeep: Collection<Emoji>?,
    ): String {
        val emojiTransformer =
            EmojiTransformer { unicodeCandidate, _ ->
                var shouldDelete = true
                if (!emojisToRemove.isNullOrEmpty() && emojisToRemove.contains(unicodeCandidate.emoji)) {
                    shouldDelete = true
                }
                if (!emojisToKeep.isNullOrEmpty() && emojisToKeep.contains(unicodeCandidate.emoji)) {
                    shouldDelete = false
                }

                if (shouldDelete) {
                    ""
                } else {
                    unicodeCandidate.emoji.getUnicode() + unicodeCandidate.getFitzpatrickUnicode()
                }
            }
        return encodeUnicode(input, emojiTransformer, null)
    }
    fun findEmojis(input: String): kotlin.collections.List<String> {
        val emojis = EmojiFactory.getUnicodeCandidates(input)
        val result = ArrayList<String>()
        for (emoji in emojis) {
            result.add(emoji.emoji.getUnicode())
        }
        return result
    }
    fun isEmoji(unicode: String): Boolean = EmojiFactory.getByUnicode(unicode) != null
}
