package com.ruishanio.taskpilot.tool.emoji.transformer

import com.ruishanio.taskpilot.tool.emoji.fitzpatrick.FitzpatrickAction
import com.ruishanio.taskpilot.tool.emoji.model.UnicodeCandidate

/**
 * Emoji 转换器。
 * 保留函数式接口，方便 Java/Kotlin 两侧都用 lambda 或匿名类挂接转换逻辑。
 */
fun interface EmojiTransformer {
    fun transform(
        unicodeCandidate: UnicodeCandidate,
        fitzpatrickAction: FitzpatrickAction?,
    ): String
}
