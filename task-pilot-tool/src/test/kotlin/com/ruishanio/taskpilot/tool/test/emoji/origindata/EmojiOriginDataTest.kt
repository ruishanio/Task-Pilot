package com.ruishanio.taskpilot.tool.test.emoji.origindata

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 原始 emoji 排序文件解析辅助。
 */
class EmojiOriginDataTest

fun main(args: Array<String>) {
    val emojiData = readLines()
    println(emojiData)
}

/**
 * 保留原有按行解析文本资源的方式，便于继续人工校验来源文件。
 */
fun readLines(): List<Map<String, String>>? {
    var reader: BufferedReader? = null
    return try {
        val lines = ArrayList<Map<String, String>>()
        reader =
            BufferedReader(
                InputStreamReader(
                    EmojiOriginDataTest::class.java.getResourceAsStream(
                        "/task-pilot-tool/emoji/origindata/emoji-ordering.txt",
                    ),
                    Charsets.UTF_8,
                ),
            )
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val currentLine = line!!
            if (currentLine.contains(";") && currentLine.contains("#")) {
                val emojiAndAlias = currentLine.substring(currentLine.indexOf("#") + 1).trim()
                val version =
                    currentLine.substring(currentLine.indexOf(";") + 1, currentLine.indexOf("#")).trim()
                val emoji = emojiAndAlias.substring(0, emojiAndAlias.indexOf(" ")).trim()
                val aliases = emojiAndAlias.substring(emojiAndAlias.indexOf(" ") + 1).trim()

                lines.add(
                    linkedMapOf(
                        "line" to currentLine,
                        "version" to version,
                        "emoji" to emoji,
                        "unicode" to "null",
                        "aliases" to aliases,
                    ),
                )
            }
        }
        lines
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        reader?.close()
    }
}
