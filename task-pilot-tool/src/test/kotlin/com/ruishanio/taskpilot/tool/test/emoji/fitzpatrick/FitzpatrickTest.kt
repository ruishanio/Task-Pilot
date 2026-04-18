package com.ruishanio.taskpilot.tool.test.emoji.fitzpatrick

import com.ruishanio.taskpilot.tool.emoji.fitzpatrick.Fitzpatrick

/**
 * Fitzpatrick Unicode 映射验证。
 */
class FitzpatrickTest

fun main(args: Array<String>) {
    println(Fitzpatrick.fitzpatrickFromUnicode("\uD83C\uDFFB"))
    println(Fitzpatrick.fitzpatrickFromUnicode("\uD83C\uDFFC"))
    println(Fitzpatrick.fitzpatrickFromUnicode("\uD83C\uDFFD"))
    println(Fitzpatrick.fitzpatrickFromUnicode("\uD83C\uDFFE"))
    println(Fitzpatrick.fitzpatrickFromUnicode("\uD83C\uDFFF"))

    println(Fitzpatrick.fitzpatrickFromUnicode("🏻"))
    println(Fitzpatrick.fitzpatrickFromUnicode("🏼"))
    println(Fitzpatrick.fitzpatrickFromUnicode("🏽"))
    println(Fitzpatrick.fitzpatrickFromUnicode("🏾"))
    println(Fitzpatrick.fitzpatrickFromUnicode("🏿"))
}
