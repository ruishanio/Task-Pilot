package com.ruishanio.taskpilot.tool.test.emoji

import com.ruishanio.taskpilot.tool.emoji.EmojiTool
import com.ruishanio.taskpilot.tool.emoji.encode.EmojiEncode
import com.ruishanio.taskpilot.tool.emoji.loader.impl.LocalEmojiDataLoader
import com.ruishanio.taskpilot.tool.emoji.model.Emoji
import com.ruishanio.taskpilot.tool.io.FileTool
import com.ruishanio.taskpilot.tool.json.GsonTool
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Comparator

/**
 * EmojiTool 编解码与数据加载验证。
 */
class EmojiToolTest {
    @Test
    fun emoji() {
        val input = "一朵美丽的茉莉🌹"
        println("unicode：$input")

        val aliases = EmojiTool.encodeUnicode(input, EmojiEncode.ALIASES)
        println("\naliases encode: $aliases")
        println("aliases decode: ${EmojiTool.decodeToUnicode(aliases, EmojiEncode.ALIASES)}")

        val decimal = EmojiTool.encodeUnicode(input, EmojiEncode.HTML_DECIMAL)
        println("\ndecimal encode: $decimal")
        println("decimal decode: ${EmojiTool.decodeToUnicode(decimal, EmojiEncode.HTML_DECIMAL)}")

        val hexDecimal = EmojiTool.encodeUnicode(input, EmojiEncode.HTML_HEX_DECIMAL)
        println("\nhexdecimal encode: $hexDecimal")
        println("hexdecimal decode: ${EmojiTool.decodeToUnicode(hexDecimal, EmojiEncode.HTML_HEX_DECIMAL)}")
    }

    @Test
    fun emoji2() {
        val originData = loadEmojiData("/task-pilot-tool/emoji/task-pilot-tool-emoji-origin.json")
        val currentData = loadEmojiData("/task-pilot-tool/emoji/task-pilot-tool-emoji.json")

        Assertions.assertEquals(GsonTool.toJson(originData), GsonTool.toJson(currentData))
    }

    /**
     * 继续按 JSON 原结构手动组装 Emoji，保证测试仍覆盖资源文件兼容性。
     */
    @Suppress("UNCHECKED_CAST")
    fun loadEmojiData(path: String): List<Emoji> {
        val resourcePath = LocalEmojiDataLoader::class.java.getResource(path)!!.path
        val emojiJson = FileTool.readString(resourcePath)
        val emojiArr = GsonTool.fromJson(emojiJson, List::class.java) as List<Any?>

        val emojis = ArrayList<Emoji>()
        for (emojiItem in emojiArr) {
            if (emojiItem is Map<*, *>) {
                val unicode = emojiItem["unicode"].toString()
                val aliases = (emojiItem["aliases"] as? List<String>) ?: emptyList()
                val tags = (emojiItem["tags"] as? List<String>) ?: emptyList()
                val supportsFitzpatrick =
                    emojiItem["supports_fitzpatrick"]?.toString()?.toBooleanStrictOrNull() ?: false
                emojis.add(Emoji(unicode, aliases, tags, supportsFitzpatrick))
            }
        }

        emojis.sortWith(Comparator.comparing(Emoji::getUnicode))
        return emojis
    }
}
