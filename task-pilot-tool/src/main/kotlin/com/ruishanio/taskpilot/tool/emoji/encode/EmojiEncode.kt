package com.ruishanio.taskpilot.tool.emoji.encode

import com.ruishanio.taskpilot.tool.emoji.fitzpatrick.FitzpatrickAction
import com.ruishanio.taskpilot.tool.emoji.transformer.EmojiTransformer

/**
 * Emoji 编码方式。
 */
enum class EmojiEncode(
    val emojiTransformer: EmojiTransformer,
) {
    ALIASES(
        EmojiTransformer { unicodeCandidate, fitzpatrickAction ->
            val finalAction = fitzpatrickAction ?: FitzpatrickAction.PARSE
            if (finalAction == FitzpatrickAction.PARSE && unicodeCandidate.hasFitzpatrick()) {
                ":" + unicodeCandidate.emoji.getAliases()[0] + "|" + unicodeCandidate.getFitzpatrickType() + ":"
            } else if (finalAction == FitzpatrickAction.IGNORE) {
                ":" + unicodeCandidate.emoji.getAliases()[0] + ":" + unicodeCandidate.getFitzpatrickUnicode()
            } else {
                ":" + unicodeCandidate.emoji.getAliases()[0] + ":"
            }
        },
    ),
    HTML_DECIMAL(
        EmojiTransformer { unicodeCandidate, fitzpatrickAction ->
            val finalAction = fitzpatrickAction ?: FitzpatrickAction.PARSE
            if (finalAction == FitzpatrickAction.IGNORE) {
                unicodeCandidate.emoji.getHtmlDecimal() + unicodeCandidate.getFitzpatrickUnicode()
            } else {
                unicodeCandidate.emoji.getHtmlDecimal()
            }
        },
    ),
    HTML_HEX_DECIMAL(
        EmojiTransformer { unicodeCandidate, fitzpatrickAction ->
            val finalAction = fitzpatrickAction ?: FitzpatrickAction.PARSE
            if (finalAction == FitzpatrickAction.IGNORE) {
                unicodeCandidate.emoji.getHtmlHexadecimal() + unicodeCandidate.getFitzpatrickUnicode()
            } else {
                unicodeCandidate.emoji.getHtmlHexadecimal()
            }
        },
    ),
}
