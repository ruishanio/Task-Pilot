package com.ruishanio.taskpilot.tool.emoji.fitzpatrick

/**
 * Fitzpatrick 肤色修饰符。
 */
enum class Fitzpatrick(
    val unicode: String,
) {
    TYPE_1_2("🏻"),
    TYPE_3("🏼"),
    TYPE_4("🏽"),
    TYPE_5("🏾"),
    TYPE_6("🏿"),
    ;

    companion object {
        fun fitzpatrickFromUnicode(unicode: String?): Fitzpatrick? {
            for (item in entries) {
                if (item.unicode == unicode) {
                    return item
                }
            }
            return null
        }
        fun fitzpatrickFromType(type: String?): Fitzpatrick? {
            return try {
                if (type == null) {
                    null
                } else {
                    valueOf(type.uppercase())
                }
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
