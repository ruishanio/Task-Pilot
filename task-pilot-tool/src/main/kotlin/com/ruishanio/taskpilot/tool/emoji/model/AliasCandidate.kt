package com.ruishanio.taskpilot.tool.emoji.model

import com.ruishanio.taskpilot.tool.emoji.fitzpatrick.Fitzpatrick

/**
 * alias 匹配结果。
 */
class AliasCandidate(
    val fullString: String,
    val alias: String,
    fitzpatrickString: String?,
) {
    val fitzpatrick: Fitzpatrick? =
        if (fitzpatrickString == null) {
            null
        } else {
            Fitzpatrick.fitzpatrickFromType(fitzpatrickString)
        }
}
