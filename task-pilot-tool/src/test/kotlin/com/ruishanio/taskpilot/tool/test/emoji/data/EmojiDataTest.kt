package com.ruishanio.taskpilot.tool.test.emoji.data

import com.ruishanio.taskpilot.tool.emoji.EmojiTool
import com.ruishanio.taskpilot.tool.emoji.encode.EmojiEncode
import com.ruishanio.taskpilot.tool.io.FileTool
import com.ruishanio.taskpilot.tool.json.GsonTool

/**
 * Emoji 数据集编码能力验证。
 */
class EmojiDataTest

fun main(args: Array<String>) {
    if (false) {
        printEmojiList()
        return
    }

    val emojiData = emojiData()

    println("test start")
    for (emoji in emojiData) {
        if (!EmojiTool.encodeUnicode(emoji, EmojiEncode.ALIASES).startsWith(":")) {
            println("1##### $emoji")
        }
        if (!EmojiTool.encodeUnicode(emoji, EmojiEncode.HTML_DECIMAL).startsWith("&#")) {
            println("2##### $emoji")
        }
        if (!EmojiTool.encodeUnicode(emoji, EmojiEncode.HTML_HEX_DECIMAL).startsWith("&#")) {
            println("3##### $emoji")
        }
    }
    println("test end")
}

/**
 * 使用现有 JSON 资源生成测试集，避免继续维护超长硬编码列表。
 */
@Suppress("UNCHECKED_CAST")
private fun emojiData(): List<String> {
    val resource = EmojiDataTest::class.java.getResource("/task-pilot-tool/emoji/task-pilot-tool-emoji.json")!!
    val json = FileTool.readString(resource.path)
    val emojiArr = GsonTool.fromJson(json, List::class.java) as List<Map<String, Any?>>
    return emojiArr.mapNotNull { it["unicode"]?.toString() }
}

@Suppress("UNCHECKED_CAST")
private fun printEmojiList() {
    val resource =
        EmojiDataTest::class.java.getResource("/task-pilot-tool/emoji/task-pilot-tool-emoji-origin.json")!!
    val json = FileTool.readString(resource.path)
    val emojiArr = GsonTool.fromJson(json, List::class.java) as List<Map<String, Any?>>
    for (item in emojiArr) {
        println(item["unicode"])
    }
}
